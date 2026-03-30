package com.example.elevator_system.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a floor in the building.
 * Tracks up/down call buttons and pending requests for that floor.
 */
public class Floor {
    private final int floorNumber;
    private boolean callUpButton;
    private boolean callDownButton;
    private final Set<Integer> waitingPassengers;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Floor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.callUpButton = false;
        this.callDownButton = false;
        this.waitingPassengers = Collections.synchronizedSet(new HashSet<>());
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public boolean isCallUpButton() {
        lock.readLock().lock();
        try {
            return callUpButton;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setCallUpButton(boolean active) {
        lock.writeLock().lock();
        try {
            this.callUpButton = active;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isCallDownButton() {
        lock.readLock().lock();
        try {
            return callDownButton;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setCallDownButton(boolean active) {
        lock.writeLock().lock();
        try {
            this.callDownButton = active;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Set<Integer> getWaitingPassengers() {
        lock.readLock().lock();
        try {
            return new HashSet<>(waitingPassengers);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addWaitingPassenger(int passengerId) {
        waitingPassengers.add(passengerId);
    }

    public void removeWaitingPassenger(int passengerId) {
        waitingPassengers.remove(passengerId);
    }

    public int getWaitingPassengerCount() {
        return waitingPassengers.size();
    }

    @Override
    public String toString() {
        return "Floor{" +
                "floorNumber=" + floorNumber +
                ", callUpButton=" + callUpButton +
                ", callDownButton=" + callDownButton +
                ", waitingPassengers=" + waitingPassengers.size() +
                '}';
    }
}

