package com.example.elevator_system.controller;

import com.example.elevator_system.dto.CallElevatorRequestDTO;
import com.example.elevator_system.dto.ElevatorStatusDTO;
import com.example.elevator_system.dto.SelectFloorRequestDTO;
import com.example.elevator_system.model.Direction;
import com.example.elevator_system.service.ElevatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for elevator operations.
 * <p>
 * Provide a way for the frontend to communicate with the backend to send requests (e.g., calling an elevator or selecting a floor) and retrieve the current state of the elevator system.
 * Ensure the system can report the status of all elevators, such as their current floor, direction, and pending requests.
 */
@RestController
@RequestMapping("/api/elevator")
public class ElevatorController {
    private static final Logger logger = LoggerFactory.getLogger(ElevatorController.class);

    @Autowired
    private ElevatorService elevatorService;

    /**
     * Call an elevator to a specific floor.
     * POST /api/elevator/call
     */
    @PostMapping("/call")
    public ResponseEntity<Map<String, Object>> callElevator(@RequestBody CallElevatorRequestDTO request) {
        try {
            logger.info("Call elevator request: floor={}, direction={}", request.getFloor(), request.getDirection());
            
            Direction direction = Direction.valueOf(request.getDirection().toUpperCase());
            elevatorService.callElevator(request.getFloor(), direction);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Elevator called successfully");
            response.put("floor", request.getFloor());
            response.put("direction", request.getDirection());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error calling elevator", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Select a floor from inside an elevator.
     * POST /api/elevator/{id}/select
     */
    @PostMapping("/{id}/select")
    public ResponseEntity<Map<String, Object>> selectFloor(@PathVariable int id,
                                                           @RequestBody SelectFloorRequestDTO request) {
        try {
            logger.info("Floor selection request: elevatorId={}, floor={}", id, request.getFloor());
            
            elevatorService.selectFloor(id, request.getFloor());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Floor selected successfully");
            response.put("elevatorId", id);
            response.put("floor", request.getFloor());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error selecting floor", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get status of all elevators.
     * GET /api/elevator/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            List<ElevatorStatusDTO> elevatorStatuses = elevatorService.getAllElevatorStatus();
            Map<Integer, Map<String, Object>> floorStatuses = elevatorService.getAllFloorsStatus();
            
            Map<String, Object> response = new HashMap<>();
            response.put("elevators", elevatorStatuses);
            response.put("floors", floorStatuses);
            response.put("config", elevatorService.getSystemConfig());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get status of a specific elevator.
     * GET /api/elevator/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getElevatorStatus(@PathVariable int id) {
        try {
            ElevatorStatusDTO status = elevatorService.getElevatorStatus(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("elevator", status);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting elevator status", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get system configuration.
     * GET /api/elevator/config
     */
    @GetMapping("/config")
    public ResponseEntity<?> getSystemConfig() {
        try {
            return ResponseEntity.ok(elevatorService.getSystemConfig());
        } catch (Exception e) {
            logger.error("Error getting configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check endpoint.
     * GET /api/elevator/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Elevator System"));
    }

    /**
     * Returns all requests currently waiting in the dispatch queue (not yet assigned to an elevator).
     * Useful for debugging — in normal operation the queue drains within one simulation tick (100 ms).
     * GET /api/elevator/queue
     */
    @GetMapping("/queue")
    public ResponseEntity<?> getPendingRequests() {
        try {
            return ResponseEntity.ok(elevatorService.getPendingRequests());
        } catch (Exception e) {
            logger.error("Error getting pending requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

