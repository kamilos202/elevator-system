package com.example.elevator_system.model;

/**
 * The four stages a door goes through in each open/close cycle:
 * CLOSED → OPENING → OPEN → CLOSING → CLOSED.
 *
 * Having intermediate OPENING and CLOSING states lets the simulation animate the
 * transition over time rather than flipping instantly, which looks more realistic
 * in the UI and also prevents the elevator from moving while the doors are still moving.
 */
public enum DoorState {
    OPEN("Open"),
    CLOSED("Closed"),
    OPENING("Opening"),
    CLOSING("Closing");

    private final String display;

    DoorState(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
