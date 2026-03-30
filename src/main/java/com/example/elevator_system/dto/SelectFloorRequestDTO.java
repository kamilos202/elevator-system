package com.example.elevator_system.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for selecting a floor inside an elevator.
 */
@Setter
@Getter
public class SelectFloorRequestDTO {
    private int floor;

    public SelectFloorRequestDTO() {}

    public SelectFloorRequestDTO(int floor) {
        this.floor = floor;
    }

    @Override
    public String toString() {
        return "SelectFloorRequestDTO{floor=" + floor + '}';
    }
}
