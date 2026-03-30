package com.example.elevator_system.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for elevator call requests.
 */
@Setter
@Getter
public class CallElevatorRequestDTO {
    private int floor;
    private String direction;

    public CallElevatorRequestDTO() {}

    public CallElevatorRequestDTO(int floor, String direction) {
        this.floor = floor;
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "CallElevatorRequestDTO{" +
                "floor=" + floor +
                ", direction='" + direction + '\'' +
                '}';
    }
}

