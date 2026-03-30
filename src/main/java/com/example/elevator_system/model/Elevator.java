package com.example.elevator_system.model;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Core domain object representing a single elevator cabin.
 *
 * All mutable fields are protected by a ReentrantReadWriteLock because the simulation
 * tick thread and the WebSocket publisher thread both read elevator state concurrently.
 * Without locking you'd occasionally see stale direction or floor values in the UI.
 *
 * The requestedFloors set is a LinkedHashSet wrapped in Collections.synchronizedSet —
 * insertion order is preserved, which keeps the SCAN algorithm's iteration predictable.
 */
public class Elevator {
    @Getter
    private final int id;
    @Getter
    private final int capacity;
    private int currentFloor;
    private Direction direction;
    private DoorState doorState;
    private final Set<Integer> requestedFloors;
    private int passengerCount;

    // Thread-safety mechanism
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private Elevator(Builder builder) {
        this.id = builder.id;
        this.capacity = builder.capacity;
        this.currentFloor = builder.currentFloor;
        this.direction = builder.direction;
        this.doorState = builder.doorState;
        this.requestedFloors = Collections.synchronizedSet(new LinkedHashSet<>(builder.requestedFloors));
        this.passengerCount = builder.passengerCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getCurrentFloor() {
        lock.readLock().lock();
        try {
            return currentFloor;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setCurrentFloor(int floor) {
        lock.writeLock().lock();
        try {
            this.currentFloor = floor;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Direction getDirection() {
        lock.readLock().lock();
        try {
            return direction;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setDirection(Direction direction) {
        lock.writeLock().lock();
        try {
            this.direction = direction;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public DoorState getDoorState() {
        lock.readLock().lock();
        try {
            return doorState;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setDoorState(DoorState doorState) {
        lock.writeLock().lock();
        try {
            this.doorState = doorState;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns a defensive copy of the requested floors so callers can't mutate
     * the internal set accidentally. The copy is also a LinkedHashSet to preserve order.
     */
    public Set<Integer> getRequestedFloors() {
        lock.readLock().lock();
        try {
            return new LinkedHashSet<>(requestedFloors);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addRequestedFloor(int floor) {
        lock.writeLock().lock();
        try {
            requestedFloors.add(floor);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeRequestedFloor(int floor) {
        lock.writeLock().lock();
        try {
            requestedFloors.remove(floor);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean hasRequestedFloor(int floor) {
        lock.readLock().lock();
        try {
            return requestedFloors.contains(floor);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getPassengerCount() {
        lock.readLock().lock();
        try {
            return passengerCount;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Caps the passenger count at capacity so we never go over the limit,
     * even if something upstream passes a bad value.
     */
    public void setPassengerCount(int count) {
        lock.writeLock().lock();
        try {
            this.passengerCount = Math.min(count, capacity);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean hasCapacity() {
        lock.readLock().lock();
        try {
            return passengerCount < capacity;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * An elevator is considered idle only when it has no direction AND no pending stops.
     * Just having direction == IDLE isn't enough — it could still be coasting to a floor.
     */
    public boolean isIdle() {
        lock.readLock().lock();
        try {
            return direction == Direction.IDLE && requestedFloors.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return "Elevator{" +
                    "id=" + id +
                    ", currentFloor=" + currentFloor +
                    ", direction=" + direction +
                    ", doorState=" + doorState +
                    ", requestedFloors=" + requestedFloors +
                    ", passengerCount=" + passengerCount +
                    '}';
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Builder for constructing Elevator instances.
     */
    public static class Builder {
        private int id;
        private int capacity = 10;
        private int currentFloor = 0;
        private Direction direction = Direction.IDLE;
        private DoorState doorState = DoorState.CLOSED;
        private Set<Integer> requestedFloors = new HashSet<>();
        private int passengerCount = 0;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder currentFloor(int floor) {
            this.currentFloor = floor;
            return this;
        }

        public Builder direction(Direction direction) {
            this.direction = direction;
            return this;
        }

        public Builder doorState(DoorState doorState) {
            this.doorState = doorState;
            return this;
        }

        public Builder requestedFloors(Set<Integer> floors) {
            this.requestedFloors = floors;
            return this;
        }

        public Builder passengerCount(int count) {
            this.passengerCount = count;
            return this;
        }

        public Elevator build() {
            return new Elevator(this);
        }
    }
}
