package com.example.elevator_system.algorithm;

import com.example.elevator_system.model.Direction;
import com.example.elevator_system.model.DoorState;
import com.example.elevator_system.model.Elevator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ScanAlgorithm.
 *
 * Each nested class covers one of the three public methods. No Spring context needed —
 * the algorithm is a plain Java class with no external dependencies.
 */
class ScanAlgorithmTest {

    private ScanAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new ScanAlgorithm();
    }

    // Small helper so test bodies stay readable
    private Elevator elevator(int floor, Direction dir, Set<Integer> requested) {
        return Elevator.builder()
                .id(0)
                .capacity(10)
                .currentFloor(floor)
                .direction(dir)
                .doorState(DoorState.CLOSED)
                .requestedFloors(requested)
                .build();
    }

    // -------------------------------------------------------------------------
    // getNextFloor
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getNextFloor")
    class GetNextFloor {

        @Test
        @DisplayName("returns null when no floors are requested")
        void returnsNullWhenQueueEmpty() {
            assertThat(algorithm.getNextFloor(elevator(3, Direction.IDLE, Set.of()), 10)).isNull();
        }

        @Test
        @DisplayName("idle elevator picks the closest requested floor")
        void idlePicksClosestFloor() {
            // At floor 5: distance to 2 is 3, distance to 9 is 4  →  should pick 2
            Elevator e = elevator(5, Direction.IDLE, Set.of(2, 9));
            assertThat(algorithm.getNextFloor(e, 10)).isEqualTo(2);
        }

        @Test
        @DisplayName("going UP picks the nearest floor still ahead")
        void goingUpPicksNearestAhead() {
            // At floor 3 going up, requests on 5 and 7  →  should pick 5 first
            Elevator e = elevator(3, Direction.UP, Set.of(5, 7));
            assertThat(algorithm.getNextFloor(e, 10)).isEqualTo(5);
        }

        @Test
        @DisplayName("going UP skips floors that are already behind")
        void goingUpSkipsFloorsBehind() {
            // At floor 5 going up, requests on 3 (behind) and 8 (ahead)  →  pick 8
            Elevator e = elevator(5, Direction.UP, Set.of(3, 8));
            assertThat(algorithm.getNextFloor(e, 10)).isEqualTo(8);
        }

        @Test
        @DisplayName("going UP reverses when nothing is left above")
        void goingUpReversesWhenNothingAhead() {
            // At floor 8 going up, only floor 2 is pending (below)  →  reverse and go to 2
            Elevator e = elevator(8, Direction.UP, Set.of(2));
            assertThat(algorithm.getNextFloor(e, 10)).isEqualTo(2);
        }

        @Test
        @DisplayName("going DOWN picks the nearest floor still below")
        void goingDownPicksNearestBelow() {
            // At floor 7 going down, requests on 2 and 4  →  should pick 4 first
            Elevator e = elevator(7, Direction.DOWN, Set.of(2, 4));
            assertThat(algorithm.getNextFloor(e, 10)).isEqualTo(4);
        }

        @Test
        @DisplayName("going DOWN reverses when nothing is left below")
        void goingDownReversesWhenNothingBelow() {
            // At floor 2 going down, only floor 7 is pending (above)  →  reverse
            Elevator e = elevator(2, Direction.DOWN, Set.of(7));
            assertThat(algorithm.getNextFloor(e, 10)).isEqualTo(7);
        }

        @Test
        @DisplayName("elevator at the only requested floor still returns that floor (door will open)")
        void alreadyAtRequestedFloor() {
            // Floor 5 is requested and we're on floor 5 — getNextFloor should return 5
            // so the door-open check fires on the same tick
            Elevator e = elevator(5, Direction.IDLE, Set.of(5));
            assertThat(algorithm.getNextFloor(e, 10)).isEqualTo(5);
        }
    }

    // -------------------------------------------------------------------------
    // determineDirection
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("determineDirection")
    class DetermineDirection {

        @Test
        @DisplayName("next floor above current  →  UP")
        void nextAboveReturnsUp() {
            Elevator e = elevator(3, Direction.IDLE, Set.of());
            assertThat(algorithm.determineDirection(e, 7, 10)).isEqualTo(Direction.UP);
        }

        @Test
        @DisplayName("next floor below current  →  DOWN")
        void nextBelowReturnsDown() {
            Elevator e = elevator(7, Direction.UP, Set.of());
            assertThat(algorithm.determineDirection(e, 3, 10)).isEqualTo(Direction.DOWN);
        }

        @Test
        @DisplayName("already at target floor keeps the current direction (avoids flip mid-cycle)")
        void alreadyAtTargetKeepsDirection() {
            Elevator e = elevator(5, Direction.UP, Set.of());
            assertThat(algorithm.determineDirection(e, 5, 10)).isEqualTo(Direction.UP);
        }

        @Test
        @DisplayName("null next floor  →  IDLE")
        void nullNextFloorReturnsIdle() {
            Elevator e = elevator(5, Direction.UP, Set.of());
            assertThat(algorithm.determineDirection(e, null, 10)).isEqualTo(Direction.IDLE);
        }
    }

    // -------------------------------------------------------------------------
    // shouldOpenDoors
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("shouldOpenDoors")
    class ShouldOpenDoors {

        @Test
        @DisplayName("returns true when current floor is in the requested set")
        void trueWhenCurrentFloorRequested() {
            Elevator e = elevator(5, Direction.UP, Set.of(5, 8));
            assertThat(algorithm.shouldOpenDoors(e)).isTrue();
        }

        @Test
        @DisplayName("returns false when current floor is not requested")
        void falseWhenCurrentFloorNotRequested() {
            Elevator e = elevator(4, Direction.UP, Set.of(5, 8));
            assertThat(algorithm.shouldOpenDoors(e)).isFalse();
        }

        @Test
        @DisplayName("returns false when no floors are requested at all")
        void falseWhenNoRequestedFloors() {
            Elevator e = elevator(5, Direction.IDLE, Set.of());
            assertThat(algorithm.shouldOpenDoors(e)).isFalse();
        }
    }
}

