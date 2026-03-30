package com.example.elevator_system.exception;

/**
 * Exception thrown when an invalid floor is requested.
 */
public class InvalidFloorException extends RuntimeException {
    public InvalidFloorException(String message) {
        super(message);
    }

    public InvalidFloorException(String message, Throwable cause) {
        super(message, cause);
    }
}

