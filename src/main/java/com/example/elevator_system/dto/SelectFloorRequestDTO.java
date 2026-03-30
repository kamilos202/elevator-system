package com.example.elevator_system.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for selecting a floor inside an elevator.
 */
@Setter
@Getter
public class SelectFloorRequestDTO {
    private int elevatorId;
    private int floor;

    public SelectFloorRequestDTO() {}

    public SelectFloorRequestDTO(int elevatorId, int floor) {
        this.elevatorId = elevatorId;
        this.floor = floor;
    }

    @Override
    public String toString() {
        return "SelectFloorRequestDTO{" +
                "elevatorId=" + elevatorId +
                ", floor=" + floor +
                '}';
    }
}

