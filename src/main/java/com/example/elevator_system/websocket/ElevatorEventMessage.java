package com.example.elevator_system.websocket;

import com.example.elevator_system.dto.ElevatorStatusDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents an elevator event that is sent via WebSocket to clients.
 */
@Setter
@Getter
public class ElevatorEventMessage {
    private String type; // ELEVATOR_MOVED, DOOR_OPENED, DOOR_CLOSED, REQUEST_PROCESSED
    private ElevatorStatusDTO elevatorStatus;
    private LocalDateTime timestamp;
    private String message;

    public ElevatorEventMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ElevatorEventMessage(String type, ElevatorStatusDTO elevatorStatus, String message) {
        this.type = type;
        this.elevatorStatus = elevatorStatus;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "ElevatorEventMessage{" +
                "type='" + type + '\'' +
                ", elevatorStatus=" + elevatorStatus +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                '}';
    }
}

