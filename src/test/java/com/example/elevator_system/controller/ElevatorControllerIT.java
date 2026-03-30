package com.example.elevator_system.controller;

import com.example.elevator_system.config.ElevatorSystemConfig;
import com.example.elevator_system.dto.ElevatorStatusDTO;
import com.example.elevator_system.exception.ElevatorException;
import com.example.elevator_system.exception.InvalidFloorException;
import com.example.elevator_system.service.ElevatorService;
import com.example.elevator_system.websocket.ElevatorEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for ElevatorController.
 *
 * Loads the full Spring context with a mock servlet environment so the real
 * WebSocket config is in place. ElevatorService and ElevatorEventPublisher are
 * both replaced by Mockito mocks — this keeps tests fast and free of scheduler noise.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ElevatorControllerIT {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private ElevatorService elevatorService;

    // Mocked so the CommandLineRunner doesn't actually start the 500ms publish loop
    @MockitoBean
    private ElevatorEventPublisher elevatorEventPublisher;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private ElevatorStatusDTO sample(int id) {
        return new ElevatorStatusDTO(id, 0, "Idle", "Closed", Set.of(), 0, 10);
    }

    // -------------------------------------------------------------------------
    // POST /api/elevator/call
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /call returns 200 and success=true for a valid request")
    void callElevator_success() throws Exception {
        doNothing().when(elevatorService).callElevator(anyInt(), any());

        mockMvc.perform(post("/api/elevator/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\": 5, \"direction\": \"UP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.floor").value(5));
    }

    @Test
    @DisplayName("POST /call returns 400 when the floor is out of range")
    void callElevator_invalidFloor() throws Exception {
        doThrow(new InvalidFloorException("Invalid floor: -1"))
                .when(elevatorService).callElevator(anyInt(), any());

        mockMvc.perform(post("/api/elevator/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\": -1, \"direction\": \"UP\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /call returns 400 when direction is not a recognised value")
    void callElevator_invalidDirection() throws Exception {
        // Direction.valueOf("SIDEWAYS") throws IllegalArgumentException in the controller
        mockMvc.perform(post("/api/elevator/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\": 3, \"direction\": \"SIDEWAYS\"}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/elevator/{id}/select
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /{id}/select returns 200 and success=true")
    void selectFloor_success() throws Exception {
        doNothing().when(elevatorService).selectFloor(anyInt(), anyInt());

        mockMvc.perform(post("/api/elevator/0/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\": 7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.elevatorId").value(0));
    }

    @Test
    @DisplayName("POST /{id}/select returns 400 when elevator doesn't exist")
    void selectFloor_unknownElevator() throws Exception {
        doThrow(new ElevatorException("Elevator not found: 99"))
                .when(elevatorService).selectFloor(eq(99), anyInt());

        mockMvc.perform(post("/api/elevator/99/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\": 3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // -------------------------------------------------------------------------
    // GET /api/elevator/status
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /status returns elevators array and floors map")
    void getStatus_success() throws Exception {
        when(elevatorService.getAllElevatorStatus()).thenReturn(List.of(sample(0), sample(1), sample(2)));
        when(elevatorService.getAllFloorsStatus()).thenReturn(Map.of());
        when(elevatorService.getSystemConfig()).thenReturn(new ElevatorSystemConfig());

        mockMvc.perform(get("/api/elevator/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elevators").isArray())
                .andExpect(jsonPath("$.elevators.length()").value(3));
    }

    // -------------------------------------------------------------------------
    // GET /api/elevator/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /{id} returns the elevator wrapped in an object")
    void getElevatorById_found() throws Exception {
        when(elevatorService.getElevatorStatus(1)).thenReturn(sample(1));

        mockMvc.perform(get("/api/elevator/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elevator.id").value(1));
    }

    @Test
    @DisplayName("GET /{id} returns 404 when elevator doesn't exist")
    void getElevatorById_notFound() throws Exception {
        when(elevatorService.getElevatorStatus(99))
                .thenThrow(new ElevatorException("Elevator not found: 99"));

        mockMvc.perform(get("/api/elevator/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // GET /api/elevator/config
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /config returns 200")
    void getConfig_success() throws Exception {
        when(elevatorService.getSystemConfig()).thenReturn(new ElevatorSystemConfig());

        mockMvc.perform(get("/api/elevator/config"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // GET /api/elevator/health
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /health returns status UP")
    void health_returnsUp() throws Exception {
        mockMvc.perform(get("/api/elevator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // -------------------------------------------------------------------------
    // GET /api/elevator/queue
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /queue returns an empty array when nothing is pending")
    void getQueue_emptyList() throws Exception {
        when(elevatorService.getPendingRequests()).thenReturn(List.of());

        mockMvc.perform(get("/api/elevator/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
