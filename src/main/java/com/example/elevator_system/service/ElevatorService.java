package com.example.elevator_system.service;

import com.example.elevator_system.algorithm.ElevatorSchedulingStrategy;
import com.example.elevator_system.algorithm.ScanAlgorithm;
import com.example.elevator_system.config.ElevatorSystemConfig;
import com.example.elevator_system.dto.ElevatorStatusDTO;
import com.example.elevator_system.exception.ElevatorException;
import com.example.elevator_system.exception.InvalidFloorException;
import com.example.elevator_system.model.*;
import com.example.elevator_system.request.RequestQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The brain of the simulation. This service owns all elevator and floor state,
 * runs the simulation loop, and handles dispatching requests to the right elevator.
 *
 * How it all fits together:
 *  - A fixed-rate scheduler fires every {@code elevatorTickIntervalMs} (default 100 ms).
 *    Each tick first drains the request queue, then advances every elevator one step
 *    through its door or movement state machine.
 *  - External hall calls and internal floor selections come in through the REST API,
 *    get wrapped in an {@link ElevatorRequest} and dropped on the priority queue.
 *    The actual elevator assignment happens on the next tick, which means we always
 *    use the freshest elevator positions when making the dispatch decision.
 *  - The {@link com.example.elevator_system.websocket.ElevatorEventPublisher} runs its own
 *    scheduler alongside this one and pushes status snapshots to the frontend over WebSocket.
 */
@Service
public class ElevatorService {
    private static final Logger logger = LoggerFactory.getLogger(ElevatorService.class);

    private final ElevatorSystemConfig config;
    private final RequestQueue requestQueue;
    private final ElevatorSchedulingStrategy schedulingStrategy;
    private final Map<Integer, Elevator> elevators;
    private final Map<Integer, Floor> floors;
    private final ScheduledExecutorService executorService;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // Timestamps for door operations (elevator id -> timestamp)
    private final Map<Integer, Long> doorOpenTimes = new ConcurrentHashMap<>();
    private final Map<Integer, Long> doorCloseTimes = new ConcurrentHashMap<>();

    @Autowired
    public ElevatorService(ElevatorSystemConfig config) {
        this.config = config;
        this.requestQueue = new RequestQueue();
        this.schedulingStrategy = new ScanAlgorithm();
        this.elevators = new ConcurrentHashMap<>();
        this.floors = new ConcurrentHashMap<>();
        this.executorService = Executors.newScheduledThreadPool(
                config.getNumberOfElevators() + 1,
                r -> {
                    Thread t = new Thread(r, "Elevator-Thread");
                    t.setDaemon(true);
                    return t;
                }
        );

        initializeSystem();
        startSimulation();
    }

    /**
     * Initializes the elevator system with elevators and floors.
     * All elevators start at ground floor (0), doors closed, direction idle.
     */
    private void initializeSystem() {
        logger.info("Initializing elevator system: {} elevators, {} floors",
                config.getNumberOfElevators(), config.getNumberOfFloors());

        // Create elevators
        for (int i = 0; i < config.getNumberOfElevators(); i++) {
            Elevator elevator = Elevator.builder()
                    .id(i)
                    .capacity(config.getElevatorCapacity())
                    .currentFloor(0)
                    .direction(Direction.IDLE)
                    .doorState(DoorState.CLOSED)
                    .build();
            elevators.put(i, elevator);
        }

        // Create floors
        for (int i = 0; i < config.getNumberOfFloors(); i++) {
            floors.put(i, new Floor(i));
        }

        logger.info("System initialized successfully");
    }

    /**
     * Kicks off the simulation loop. The executor runs on a daemon thread so it doesn't
     * prevent the JVM from shutting down if something goes wrong.
     */
    private void startSimulation() {
        logger.info("Starting elevator simulation");
        // Schedule elevator movement at fixed intervals
        executorService.scheduleAtFixedRate(
                this::simulationTick,
                0,
                config.getElevatorTickIntervalMs(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * One simulation step: dispatch queued requests first, then process each elevator.
     * Keeping dispatch before movement ensures a request that arrived between ticks
     * doesn't have to wait an extra tick before the elevator starts moving.
     */
    private void simulationTick() {
        try {
            dispatchPendingRequests();
            for (Elevator elevator : elevators.values()) {
                processElevator(elevator);
            }
        } catch (Exception e) {
            logger.error("Error in simulation tick", e);
        }
    }

    /**
     * Drains the request queue and assigns each pending request to the right elevator.
     *
     * CALL requests (elevatorId == -1) go through {@link #findBestElevator} so the
     * assignment is made with the freshest elevator state, not the state at call time.
     * SELECT requests already carry the target elevatorId and are routed directly.
     * After assignment the floor is added to that elevator's requestedFloors set,
     * which is what the SCAN algorithm reads on every tick.
     */
    private void dispatchPendingRequests() {
        int pending = requestQueue.size();
        if (pending == 0) return;

        logger.debug("Dispatching {} pending request(s) from queue", pending);
        while (!requestQueue.isEmpty()) {
            ElevatorRequest request = requestQueue.poll();
            if (request == null) break;

            int targetElevatorId;
            if (request.getElevatorId() >= 0) {
                // SELECT: passenger is already riding a specific elevator, route directly
                targetElevatorId = request.getElevatorId();
            } else {
                // CALL: pick the best available elevator based on current positions
                targetElevatorId = findBestElevator(request.getRequestedFloor(), request.getDirection());
                logger.info("Dispatched floor-{} {} call to elevator {}",
                        request.getRequestedFloor(), request.getDirection(), targetElevatorId);
            }

            Elevator elevator = elevators.get(targetElevatorId);
            if (elevator != null) {
                elevator.addRequestedFloor(request.getRequestedFloor());
            }
        }
    }

    /**
     * Runs one cycle of the door/movement state machine for a single elevator.
     * Door transitions always take priority over movement — we won't move
     * while the doors are doing anything other than being fully closed.
     */
    private void processElevator(Elevator elevator) {
        DoorState currentDoorState = elevator.getDoorState();

        // Handle door operations first
        if (currentDoorState == DoorState.OPENING) {
            handleDoorOpening(elevator);
        } else if (currentDoorState == DoorState.CLOSING) {
            handleDoorClosing(elevator);
        } else if (currentDoorState == DoorState.OPEN) {
            handleOpenDoor(elevator);
        } else if (currentDoorState == DoorState.CLOSED) {
            handleClosedDoor(elevator);
        }
    }

    /**
     * OPENING state: wait until doorOpenCloseTimeMs has elapsed, then flip to OPEN.
     * We record the start time on first entry so subsequent ticks just check the delta.
     */
    private void handleDoorOpening(Elevator elevator) {
        long currentTime = System.currentTimeMillis();
        long openStartTime = doorOpenTimes.getOrDefault(elevator.getId(), currentTime);
        doorOpenTimes.put(elevator.getId(), openStartTime);

        if (currentTime - openStartTime >= config.getDoorOpenCloseTimeMs()) {
            elevator.setDoorState(DoorState.OPEN);
            doorOpenTimes.remove(elevator.getId());
        }
    }

    /**
     * OPEN state: give passengers time to board/exit (doorStayOpenTimeMs), then
     * remove this floor from the requested set and start closing.
     * We also clear the hall button here — the floor has been served.
     */
    private void handleOpenDoor(Elevator elevator) {
        long currentTime = System.currentTimeMillis();

        if (!doorOpenTimes.containsKey(elevator.getId())) {
            doorOpenTimes.put(elevator.getId(), currentTime);
            // Reset the floor call button when elevator arrives
            Floor floorObj = floors.get(elevator.getCurrentFloor());
            if (floorObj != null) {
                floorObj.setCallUpButton(false);
                floorObj.setCallDownButton(false);
            }
        }

        long openStartTime = doorOpenTimes.get(elevator.getId());

        // After door stay time, close the door
        if (currentTime - openStartTime >= config.getDoorStayOpenTimeMs()) {
            // Remove the served floor from requests
            elevator.removeRequestedFloor(elevator.getCurrentFloor());
            // Start closing
            elevator.setDoorState(DoorState.CLOSING);
            doorCloseTimes.put(elevator.getId(), System.currentTimeMillis());
            doorOpenTimes.remove(elevator.getId());
        }
    }

    /**
     * CLOSING state: mirror of opening. Once the animation time has passed, flip to CLOSED.
     */
    private void handleDoorClosing(Elevator elevator) {
        long currentTime = System.currentTimeMillis();
        long closeStartTime = doorCloseTimes.getOrDefault(elevator.getId(), currentTime);
        doorCloseTimes.put(elevator.getId(), closeStartTime);

        if (currentTime - closeStartTime >= config.getDoorOpenCloseTimeMs()) {
            elevator.setDoorState(DoorState.CLOSED);
            doorCloseTimes.remove(elevator.getId());
        }
    }

    /**
     * CLOSED state: this is where movement happens.
     * First ask the scheduling strategy if we should open doors at the current floor
     * (i.e. this floor is in requestedFloors). If not, ask for the next floor to head
     * toward and move one floor in that direction. If there's nothing to do, go idle.
     */
    private void handleClosedDoor(Elevator elevator) {
        // Check if we need to open doors at current floor
        if (schedulingStrategy.shouldOpenDoors(elevator)) {
            logger.debug("Elevator {} opening doors at floor {}", elevator.getId(), elevator.getCurrentFloor());
            elevator.setDoorState(DoorState.OPENING);
            doorOpenTimes.put(elevator.getId(), System.currentTimeMillis());
            return;
        }

        // Get next floor from scheduler
        Integer nextFloor = schedulingStrategy.getNextFloor(elevator, config.getNumberOfFloors());
        
        if (nextFloor != null) {
            Direction direction = schedulingStrategy.determineDirection(elevator, nextFloor, config.getNumberOfFloors());
            elevator.setDirection(direction);
            
            logger.debug("Elevator {} moving from {} to {} (direction: {})", 
                    elevator.getId(), elevator.getCurrentFloor(), nextFloor, direction);

            // Move one floor per tick
            int currentFloor = elevator.getCurrentFloor();
            if (nextFloor > currentFloor) {
                elevator.setCurrentFloor(currentFloor + 1);
            } else if (nextFloor < currentFloor) {
                elevator.setCurrentFloor(currentFloor - 1);
            }
        } else {
            // No more requests - stay idle
            elevator.setDirection(Direction.IDLE);
        }
    }

    /**
     * Called when someone presses the hall call button on a floor.
     * We enqueue the request and light up the button immediately — the actual
     * elevator assignment happens during the next simulation tick.
     */
    public void callElevator(int floor, Direction direction) {
        validateFloor(floor);

        logger.info("Elevator called to floor {} ({})", floor, direction);

        ElevatorRequest request = ElevatorRequest.builder()
                .id(requestQueue.generateRequestId())
                .requestedFloor(floor)
                .type(RequestType.CALL)
                .direction(direction)
                .elevatorId(-1)   // will be assigned in dispatchPendingRequests()
                .priority(1)
                .build();

        requestQueue.addRequest(request);

        // Light up the hall button immediately so the UI reflects the pending call
        Floor floorObj = floors.get(floor);
        if (floorObj != null) {
            if (direction == Direction.UP) {
                floorObj.setCallUpButton(true);
            } else if (direction == Direction.DOWN) {
                floorObj.setCallDownButton(true);
            }
        }
    }

    /**
     * Called when a passenger inside an elevator presses a floor button.
     * We already know which elevator, so the request is queued with the elevatorId
     * set — the dispatch step won't need to run the scoring logic for it.
     * Internal selections have higher priority (2) than hall calls (1) so a passenger
     * already on board doesn't get held up by new external requests.
     */
    public void selectFloor(int elevatorId, int floor) {
        validateFloor(floor);

        if (!elevators.containsKey(elevatorId)) {
            throw new ElevatorException("Elevator not found: " + elevatorId);
        }

        logger.info("Floor {} selected inside elevator {}", floor, elevatorId);

        ElevatorRequest request = ElevatorRequest.builder()
                .id(requestQueue.generateRequestId())
                .requestedFloor(floor)
                .type(RequestType.SELECT)
                .direction(Direction.IDLE)
                .elevatorId(elevatorId)  // already known — skip dispatch scoring
                .priority(2)             // internal selections take priority over hall calls
                .build();

        requestQueue.addRequest(request);
    }

    /**
     * Scores every elevator and returns the id of the best one for a given call.
     *
     * Scoring (lower is better):
     *   idle elevator                              → distance
     *   already moving the right way, floor ahead  → distance + 5
     *   anything else with spare capacity          → distance + 20
     *
     * Ties are broken by lowest elevator ID so the result is always deterministic
     * regardless of HashMap iteration order.
     */
    private int findBestElevator(int floor, Direction requestedDirection) {
        Elevator bestElevator = null;
        int bestScore = Integer.MAX_VALUE;

        for (Elevator elevator : elevators.values()) {
            if (!elevator.hasCapacity()) continue;

            int distance = Math.abs(elevator.getCurrentFloor() - floor);
            int score;

            if (elevator.isIdle()) {
                // Best: idle elevator – goes wherever needed
                score = distance;
            } else if (shouldAddToElevator(elevator, floor, requestedDirection)) {
                // Good: already moving in the right direction and floor is still ahead
                score = distance + 5;
            } else {
                // Fallback: any other elevator with capacity
                score = distance + 20;
            }

            // Prefer lower score; break ties with lower elevator ID
            if (score < bestScore || (score == bestScore && bestElevator != null && elevator.getId() < bestElevator.getId())) {
                bestElevator = elevator;
                bestScore = score;
            }
        }

        return bestElevator != null ? bestElevator.getId() : 0;
    }

    /**
     * Returns true if it makes sense to add this floor to an elevator that's already moving.
     * The floor has to be ahead of the elevator and the requested direction must match —
     * you wouldn't add a downward request to an elevator that's still going up.
     */
    private boolean shouldAddToElevator(Elevator elevator, int floor, Direction direction) {
        Direction elevatorDir = elevator.getDirection();
        int currentFloor = elevator.getCurrentFloor();

        if (elevatorDir == Direction.UP) {
            return floor >= currentFloor && direction == Direction.UP;
        } else if (elevatorDir == Direction.DOWN) {
            return floor <= currentFloor && direction == Direction.DOWN;
        }
        return false;
    }

    /**
     * Gets the current status of all elevators.
     */
    public List<ElevatorStatusDTO> getAllElevatorStatus() {
        lock.readLock().lock();
        try {
            List<ElevatorStatusDTO> statusList = new ArrayList<>();
            for (Elevator elevator : elevators.values()) {
                statusList.add(convertToDTO(elevator));
            }
            return statusList;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the status of a specific elevator.
     */
    public ElevatorStatusDTO getElevatorStatus(int elevatorId) {
        if (!elevators.containsKey(elevatorId)) {
            throw new ElevatorException("Elevator not found: " + elevatorId);
        }
        return convertToDTO(elevators.get(elevatorId));
    }

    /**
     * Converts an Elevator to ElevatorStatusDTO.
     */
    private ElevatorStatusDTO convertToDTO(Elevator elevator) {
        return new ElevatorStatusDTO(
                elevator.getId(),
                elevator.getCurrentFloor(),
                elevator.getDirection().getDisplay(),
                elevator.getDoorState().getDisplay(),
                elevator.getRequestedFloors(),
                elevator.getPassengerCount(),
                elevator.getCapacity()
        );
    }

    /**
     * Gets the status of all floors.
     */
    public Map<Integer, Map<String, Object>> getAllFloorsStatus() {
        Map<Integer, Map<String, Object>> statusMap = new TreeMap<>();
        
        for (Floor floor : floors.values()) {
            Map<String, Object> floorStatus = new HashMap<>();
            floorStatus.put("floorNumber", floor.getFloorNumber());
            floorStatus.put("callUpButton", floor.isCallUpButton());
            floorStatus.put("callDownButton", floor.isCallDownButton());
            floorStatus.put("waitingPassengers", floor.getWaitingPassengerCount());
            statusMap.put(floor.getFloorNumber(), floorStatus);
        }
        
        return statusMap;
    }

    /**
     * Gets system configuration.
     */
    public ElevatorSystemConfig getSystemConfig() {
        return config;
    }

    /**
     * Returns a snapshot of all requests currently waiting in the dispatch queue.
     * In normal operation this is empty because the queue drains every 100 ms,
     * but it's useful for the debug endpoint and checking system health.
     */
    public List<ElevatorRequest> getPendingRequests() {
        return requestQueue.getAllRequests();
    }

    /**
     * Validates if a floor number is valid.
     */
    private void validateFloor(int floor) {
        if (floor < 0 || floor >= config.getNumberOfFloors()) {
            throw new InvalidFloorException(
                    String.format("Invalid floor: %d. Building has floors 0-%d",
                    floor, config.getNumberOfFloors() - 1)
            );
        }
    }

    /**
     * Stops the simulation. Clears any unprocessed requests first so we don't
     * accidentally dispatch them if the executor somehow lingers.
     */
    public void shutdown() {
        logger.info("Shutting down elevator system");
        requestQueue.clear(); // Discard any requests that haven't been dispatched yet
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
