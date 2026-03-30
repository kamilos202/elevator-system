package com.example.elevator_system.websocket;

import com.example.elevator_system.dto.ElevatorStatusDTO;
import java.time.LocalDateTime;

/**
 * Represents an elevator event that is sent via WebSocket to clients.
 */
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ElevatorStatusDTO getElevatorStatus() {
        return elevatorStatus;
    }

    public void setElevatorStatus(ElevatorStatusDTO elevatorStatus) {
        this.elevatorStatus = elevatorStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

