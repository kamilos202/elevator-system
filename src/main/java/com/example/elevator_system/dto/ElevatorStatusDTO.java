package com.example.elevator_system.dto;

import java.util.Set;

/**
 * Data Transfer Object for elevator status information.
 */
public class ElevatorStatusDTO {
    private int id;
    private int currentFloor;
    private String direction;
    private String doorState;
    private Set<Integer> requestedFloors;
    private int passengerCount;
    private int capacity;

    public ElevatorStatusDTO() {}

    public ElevatorStatusDTO(int id, int currentFloor, String direction, String doorState,
                            Set<Integer> requestedFloors, int passengerCount, int capacity) {
        this.id = id;
        this.currentFloor = currentFloor;
        this.direction = direction;
        this.doorState = doorState;
        this.requestedFloors = requestedFloors;
        this.passengerCount = passengerCount;
        this.capacity = capacity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDoorState() {
        return doorState;
    }

    public void setDoorState(String doorState) {
        this.doorState = doorState;
    }

    public Set<Integer> getRequestedFloors() {
        return requestedFloors;
    }

    public void setRequestedFloors(Set<Integer> requestedFloors) {
        this.requestedFloors = requestedFloors;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = passengerCount;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return "ElevatorStatusDTO{" +
                "id=" + id +
                ", currentFloor=" + currentFloor +
                ", direction='" + direction + '\'' +
                ", doorState='" + doorState + '\'' +
                ", requestedFloors=" + requestedFloors +
                ", passengerCount=" + passengerCount +
                ", capacity=" + capacity +
                '}';
    }
}

