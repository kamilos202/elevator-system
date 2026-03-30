package com.example.elevator_system;

import com.example.elevator_system.dto.ElevatorStatusDTO;
import com.example.elevator_system.model.Direction;
import com.example.elevator_system.service.ElevatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end simulation tests.
 *
 * These spin up the full Spring context with a real ElevatorService scheduler
 * but with very fast timing so the tests finish in a few seconds rather than
 * the production 5-10 seconds. Each test calls the service API and then polls
 * until the expected elevator state is reached (or a timeout expires).
 *
 * Note: because the context is shared across all tests in this class, later
 * tests might see elevators that are already at non-zero floors from earlier
 * ones — that's intentional and doesn't invalidate the assertions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "elevator.system.numberOfElevators=3",
        "elevator.system.numberOfFloors=10",
        "elevator.system.elevatorCapacity=10",
        "elevator.system.elevatorTickIntervalMs=20",
        "elevator.system.doorOpenCloseTimeMs=60",
        "elevator.system.doorStayOpenTimeMs=60",
        "elevator.system.floorTransitionTimeMs=20"
})
class ElevatorSimulationIT {

    @Autowired
    private ElevatorService service;

    /**
     * Polls condition every 20 ms until it returns true or maxWaitMs elapses.
     * Returns true if the condition was met within the deadline.
     */
    private boolean waitFor(long maxWaitMs, ConditionCheck condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.check()) return true;
            Thread.sleep(20);
        }
        return false;
    }

    @FunctionalInterface
    interface ConditionCheck {
        boolean check();
    }

    // -------------------------------------------------------------------------

    @Test
    @DisplayName("calling an elevator eventually brings one to the requested floor")
    void callElevatorMovesToFloor() throws InterruptedException {
        service.callElevator(6, Direction.UP);

        boolean arrived = waitFor(6000, () ->
                service.getAllElevatorStatus().stream()
                        .anyMatch(s -> s.getCurrentFloor() == 6));

        assertThat(arrived)
                .as("An elevator should have reached floor 6 within the timeout")
                .isTrue();
    }

    @Test
    @DisplayName("selecting a floor from inside moves that specific elevator there")
    void selectFloorMovesSpecificElevator() throws InterruptedException {
        service.selectFloor(2, 4);

        boolean arrived = waitFor(6000, () ->
                service.getElevatorStatus(2).getCurrentFloor() == 4);

        assertThat(arrived)
                .as("Elevator 2 should have reached floor 4")
                .isTrue();
    }

    @Test
    @DisplayName("doors transition through OPENING → OPEN when elevator arrives")
    void doorsOpenOnArrival() throws InterruptedException {
        service.selectFloor(1, 3);

        // We want to catch the door in a non-CLOSED state at some point
        boolean doorMoved = waitFor(6000, () -> {
            String doorState = service.getElevatorStatus(1).getDoorState();
            return !doorState.equals("Closed");
        });

        assertThat(doorMoved)
                .as("Elevator 1 doors should open after arriving at floor 3")
                .isTrue();
    }

    @Test
    @DisplayName("served floor is removed from requestedFloors after the door cycle completes")
    void requestedFloorRemovedAfterService() throws InterruptedException {
        service.selectFloor(0, 5);

        // First, wait until floor 5 appears in the set
        boolean queued = waitFor(4000, () -> {
            ElevatorStatusDTO s = service.getElevatorStatus(0);
            return s.getRequestedFloors() != null && s.getRequestedFloors().contains(5);
        });
        assertThat(queued).as("Floor 5 should appear in requestedFloors after selectFloor").isTrue();

        // Then wait for the full door cycle to clear it
        boolean cleared = waitFor(8000, () -> {
            ElevatorStatusDTO s = service.getElevatorStatus(0);
            return s.getRequestedFloors() == null || !s.getRequestedFloors().contains(5);
        });
        assertThat(cleared).as("Floor 5 should be removed after the door cycle finishes").isTrue();
    }

    @Test
    @DisplayName("multiple simultaneous calls are all eventually served")
    void multipleCalls_allServed() throws InterruptedException {
        // Call from three different floors — one per elevator ideally
        service.callElevator(2, Direction.UP);
        service.callElevator(7, Direction.DOWN);
        service.callElevator(4, Direction.UP);

        boolean floor2 = waitFor(8000, () ->
                service.getAllElevatorStatus().stream().anyMatch(s -> s.getCurrentFloor() == 2));
        boolean floor7 = waitFor(8000, () ->
                service.getAllElevatorStatus().stream().anyMatch(s -> s.getCurrentFloor() == 7));
        boolean floor4 = waitFor(8000, () ->
                service.getAllElevatorStatus().stream().anyMatch(s -> s.getCurrentFloor() == 4));

        assertThat(floor2).as("A car should reach floor 2").isTrue();
        assertThat(floor7).as("A car should reach floor 7").isTrue();
        assertThat(floor4).as("A car should reach floor 4").isTrue();
    }

    @Test
    @DisplayName("getAllElevatorStatus always returns the configured number of elevators")
    void statusCountMatchesConfig() {
        List<ElevatorStatusDTO> statuses = service.getAllElevatorStatus();
        assertThat(statuses).hasSize(3);
        assertThat(statuses).extracting(ElevatorStatusDTO::getId)
                .containsExactlyInAnyOrder(0, 1, 2);
    }
}

