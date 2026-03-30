package com.example.elevator_system.algorithm;

import com.example.elevator_system.model.Direction;
import com.example.elevator_system.model.Elevator;

/**
 * Contract that any elevator scheduling algorithm has to fulfil.
 *
 * The three methods map directly to the questions the simulation tick needs answered
 * each time it processes an elevator:
 *  1. Where should I go next?
 *  2. Which way is that?
 *  3. Should I stop and open the doors right here?
 *
 * Currently only ScanAlgorithm is wired in, but swapping in a different strategy
 * (e.g. nearest-car, destination dispatch) is just a matter of providing another
 * implementation and injecting it into ElevatorService.
 */
public interface ElevatorSchedulingStrategy {

    /**
     * Returns the next floor this elevator should travel to, or {@code null} if the
     * requested-floors set is empty and there's nothing to do.
     *
     * @param elevator    the elevator being evaluated
     * @param totalFloors total floors in the building (used as an upper bound)
     */
    Integer getNextFloor(Elevator elevator, int totalFloors);

    /**
     * Given that we've already decided on a target floor, work out which direction
     * the elevator needs to travel. Returns IDLE if nextFloor is null.
     *
     * @param elevator    the elevator being evaluated
     * @param nextFloor   the floor returned by getNextFloor (may be null)
     * @param totalFloors total floors in the building
     */
    Direction determineDirection(Elevator elevator, Integer nextFloor, int totalFloors);

    /**
     * Quick yes/no: should the doors open at the elevator's current floor?
     * Called before movement so we don't accidentally skip a stop.
     *
     * @param elevator the elevator being evaluated
     */
    boolean shouldOpenDoors(Elevator elevator);
}
