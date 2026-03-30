package com.example.elevator_system.service;

import com.example.elevator_system.dto.ElevatorStatusDTO;
import com.example.elevator_system.exception.ElevatorException;
import com.example.elevator_system.exception.InvalidFloorException;
import com.example.elevator_system.model.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spring context tests for ElevatorService.
 *
 * These run with a real Spring context (webEnvironment = NONE so no HTTP server starts)
 * but with very fast timing so the background scheduler doesn't slow the tests down.
 * The focus is on the public API — validation, exception handling, and status queries.
 * End-to-end movement is covered in ElevatorSimulationIT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "elevator.system.numberOfElevators=3",
        "elevator.system.numberOfFloors=10",
        "elevator.system.elevatorCapacity=10",
        "elevator.system.elevatorTickIntervalMs=50",
        "elevator.system.doorOpenCloseTimeMs=100",
        "elevator.system.doorStayOpenTimeMs=100",
        "elevator.system.floorTransitionTimeMs=100"
})
class ElevatorServiceTest {

    @Autowired
    private ElevatorService service;

    // -------------------------------------------------------------------------
    // callElevator
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("callElevator")
    class CallElevator {

        @Test
        @DisplayName("valid floor and direction completes without exception")
        void validCallSucceeds() {
            service.callElevator(5, Direction.UP);
        }

        @Test
        @DisplayName("floor -1 throws InvalidFloorException")
        void negativeFloorThrows() {
            assertThatThrownBy(() -> service.callElevator(-1, Direction.UP))
                    .isInstanceOf(InvalidFloorException.class);
        }

        @Test
        @DisplayName("floor equal to numberOfFloors (out of range) throws InvalidFloorException")
        void floorAtUpperBoundThrows() {
            // building has floors 0-9, so floor 10 is invalid
            assertThatThrownBy(() -> service.callElevator(10, Direction.UP))
                    .isInstanceOf(InvalidFloorException.class);
        }

        @Test
        @DisplayName("floor 0 and floor 9 (boundary values) are both valid")
        void boundaryFloorsAreValid() {
            service.callElevator(0, Direction.UP);
            service.callElevator(9, Direction.DOWN);
        }
    }

    // -------------------------------------------------------------------------
    // selectFloor
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("selectFloor")
    class SelectFloor {

        @Test
        @DisplayName("valid elevator and floor completes without exception")
        void validSelectionSucceeds() {
            service.selectFloor(0, 3);
        }

        @Test
        @DisplayName("unknown elevator ID throws ElevatorException")
        void unknownElevatorThrows() {
            assertThatThrownBy(() -> service.selectFloor(99, 5))
                    .isInstanceOf(ElevatorException.class)
                    .hasMessageContaining("Elevator not found");
        }

        @Test
        @DisplayName("invalid floor throws InvalidFloorException regardless of elevator")
        void invalidFloorThrows() {
            assertThatThrownBy(() -> service.selectFloor(0, -1))
                    .isInstanceOf(InvalidFloorException.class);
        }

        @Test
        @DisplayName("all three elevators accept a valid selection")
        void allElevatorsAcceptSelection() {
            service.selectFloor(0, 2);
            service.selectFloor(1, 4);
            service.selectFloor(2, 6);
        }
    }

    // -------------------------------------------------------------------------
    // getAllElevatorStatus
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getAllElevatorStatus")
    class GetAllStatus {

        @Test
        @DisplayName("returns one entry per configured elevator")
        void returnsCorrectCount() {
            assertThat(service.getAllElevatorStatus()).hasSize(3);
        }

        @Test
        @DisplayName("elevator IDs start at 0 and are contiguous")
        void elevatorIdsAreContiguous() {
            List<ElevatorStatusDTO> statuses = service.getAllElevatorStatus();
            assertThat(statuses).extracting(ElevatorStatusDTO::getId)
                    .containsExactlyInAnyOrder(0, 1, 2);
        }

        @Test
        @DisplayName("each status has capacity from config")
        void capacityMatchesConfig() {
            service.getAllElevatorStatus().forEach(s ->
                    assertThat(s.getCapacity()).isEqualTo(10));
        }
    }

    // -------------------------------------------------------------------------
    // getElevatorStatus
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getElevatorStatus")
    class GetSingleStatus {

        @Test
        @DisplayName("returns the right elevator for a valid ID")
        void validIdReturnsElevator() {
            ElevatorStatusDTO status = service.getElevatorStatus(1);
            assertThat(status.getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("throws ElevatorException for an unknown ID")
        void unknownIdThrows() {
            assertThatThrownBy(() -> service.getElevatorStatus(99))
                    .isInstanceOf(ElevatorException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getPendingRequests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getPendingRequests never returns null")
    void getPendingRequestsNotNull() {
        assertThat(service.getPendingRequests()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // getSystemConfig
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getSystemConfig reflects the test property values")
    void configReflectsTestProperties() {
        var cfg = service.getSystemConfig();
        assertThat(cfg.getNumberOfElevators()).isEqualTo(3);
        assertThat(cfg.getNumberOfFloors()).isEqualTo(10);
        assertThat(cfg.getElevatorCapacity()).isEqualTo(10);
    }
}

