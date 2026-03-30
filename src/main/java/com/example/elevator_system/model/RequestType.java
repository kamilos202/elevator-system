package com.example.elevator_system.model;

import lombok.Getter;

/**
 * Whether a request originated from someone outside (pressing the hall button)
 * or from inside (pressing a floor button on the panel).
 *
 * This distinction matters for priority: SELECT requests (already-riding passengers)
 * are given priority 2 while CALL requests get priority 1, so a passenger on board
 * won't get stuck waiting for new hall calls to be served first.
 */
@Getter
public enum RequestType {
    CALL("External call from floor"),
    SELECT("Internal floor selection");

    private final String description;

    RequestType(String description) {
        this.description = description;
    }
}
