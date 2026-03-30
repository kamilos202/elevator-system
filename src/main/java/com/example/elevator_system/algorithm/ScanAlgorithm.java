package com.example.elevator_system.algorithm;

import com.example.elevator_system.model.Direction;
import com.example.elevator_system.model.Elevator;

import java.util.Comparator;
import java.util.Set;

/**
 * SCAN algorithm — the classic "elevator algorithm" you'll find in OS textbooks.
 *
 * The idea is pretty simple: the elevator sweeps in one direction, stopping at every
 * requested floor it passes through, and only reverses when there's nothing left ahead.
 * This avoids the starvation you'd get with a naive "nearest-first" approach, because
 * no floor gets skipped indefinitely.
 *
 * One thing worth noting: when the elevator is idle and a new request comes in,
 * we just head straight to the closest floor — no point picking a direction before
 * we know where we're going.
 */
public class ScanAlgorithm implements ElevatorSchedulingStrategy {

    /**
     * Picks the next floor this elevator should move toward.
     *
     * If the elevator is idle we go to whichever requested floor is closest.
     * Otherwise we keep going in the current direction and grab the nearest floor
     * that's still ahead of us. If there's nothing ahead, we reverse and pick up
     * floors coming the other way. Returns null when the queue is empty.
     */
    @Override
    public Integer getNextFloor(Elevator elevator, int totalFloors) {
        Set<Integer> requestedFloors = elevator.getRequestedFloors();

        if (requestedFloors.isEmpty()) {
            return null; // nothing to do
        }

        int currentFloor = elevator.getCurrentFloor();
        Direction currentDirection = elevator.getDirection();

        // Idle means we haven't committed to a direction yet — just go to the closest floor
        if (currentDirection == Direction.IDLE) {
            return findClosestFloor(currentFloor, requestedFloors);
        }

        // Try to keep moving the way we're already going
        Integer nextInDirection = null;
        if (currentDirection == Direction.UP) {
            nextInDirection = findNextFloorInDirection(currentFloor, requestedFloors, true, totalFloors);
        } else if (currentDirection == Direction.DOWN) {
            nextInDirection = findNextFloorInDirection(currentFloor, requestedFloors, false, 0);
        }

        if (nextInDirection != null) {
            return nextInDirection;
        }

        // Nothing left in this direction — time to reverse
        if (currentDirection == Direction.UP) {
            return findNextFloorInDirection(currentFloor, requestedFloors, false, 0);
        } else {
            return findNextFloorInDirection(currentFloor, requestedFloors, true, totalFloors);
        }
    }

    /**
     * Returns whichever floor in the set is physically closest to where we are now.
     * Used when the elevator just woke up from idle.
     */
    private Integer findClosestFloor(int currentFloor, Set<Integer> requestedFloors) {
        return requestedFloors.stream()
                .min(Comparator.comparingInt(floor -> Math.abs(floor - currentFloor)))
                .orElse(null);
    }

    /**
     * Finds the next stop in a given direction.
     *
     * When going up, we want the smallest floor that's strictly above us (and within bounds).
     * When going down, we want the largest floor that's strictly below us.
     * Returns null if there's no pending stop in that direction.
     *
     * @param going  true = going up, false = going down
     * @param limit  the boundary to stay within (top floor when going up, 0 when going down)
     */
    private Integer findNextFloorInDirection(int currentFloor, Set<Integer> requestedFloors,
                                             boolean going, int limit) {
        if (going) {
            return requestedFloors.stream()
                    .filter(floor -> floor > currentFloor && floor <= limit)
                    .min(Integer::compareTo)
                    .orElse(null);
        } else {
            return requestedFloors.stream()
                    .filter(floor -> floor < currentFloor && floor >= limit)
                    .max(Integer::compareTo)
                    .orElse(null);
        }
    }

    /**
     * Works out which way the elevator needs to move to reach {@code nextFloor}.
     * If it's already there, we keep the current direction — changing direction
     * mid-floor would just confuse the door logic.
     */
    @Override
    public Direction determineDirection(Elevator elevator, Integer nextFloor, int totalFloors) {
        if (nextFloor == null) {
            return Direction.IDLE;
        }

        int currentFloor = elevator.getCurrentFloor();
        if (nextFloor > currentFloor) {
            return Direction.UP;
        } else if (nextFloor < currentFloor) {
            return Direction.DOWN;
        } else {
            // Already at the target floor, hold direction until the door cycle finishes
            return elevator.getDirection();
        }
    }

    /**
     * Simple check: if the current floor is in the requested-floors set, open the doors.
     * The service layer handles the actual door state machine; this just answers "should we?"
     */
    @Override
    public boolean shouldOpenDoors(Elevator elevator) {
        return elevator.getRequestedFloors().contains(elevator.getCurrentFloor());
    }
}

