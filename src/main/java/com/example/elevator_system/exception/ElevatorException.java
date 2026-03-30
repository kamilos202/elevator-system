package com.example.elevator_system.exception;

/**
 * Exception thrown when an elevator operation fails.
 */
public class ElevatorException extends RuntimeException {
    public ElevatorException(String message) {
        super(message);
    }

    public ElevatorException(String message, Throwable cause) {
        super(message, cause);
    }
}

