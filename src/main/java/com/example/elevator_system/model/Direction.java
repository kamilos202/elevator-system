package com.example.elevator_system.model;

/**
 * Which way the elevator is currently travelling, or IDLE if it's just sitting there.
 * The display string is what gets serialised to the frontend, so keep it human-readable.
 */
public enum Direction {
    UP("Up"),
    DOWN("Down"),
    IDLE("Idle");

    private final String display;

    Direction(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
