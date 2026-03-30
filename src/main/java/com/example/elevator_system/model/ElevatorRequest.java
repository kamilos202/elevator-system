package com.example.elevator_system.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a single elevator request — either someone calling the elevator from
 * a floor, or a passenger inside selecting their destination.
 *
 * Immutable by design: once created, the request doesn't change. This makes it safe
 * to pass around between threads without any extra synchronisation.
 *
 * elevatorId == -1 means the request is a CALL that hasn't been assigned to a specific
 * elevator yet. The dispatch step in ElevatorService picks one on the next tick.
 * Any other elevatorId means it's a SELECT — the passenger is already on board and
 * we know exactly which elevator to route it to.
 */
@Getter
public class ElevatorRequest implements Comparable<ElevatorRequest> {
    private final int id;
    private final int requestedFloor;
    private final RequestType type;
    private final Direction direction;
    private final LocalDateTime createdAt;
    private final int priority;
    /** Which elevator should serve this request. {@code -1} means not yet assigned (CALL). */
    private final int elevatorId;

    private ElevatorRequest(Builder builder) {
        this.id = builder.id;
        this.requestedFloor = builder.requestedFloor;
        this.type = builder.type;
        this.direction = builder.direction;
        this.createdAt = builder.createdAt;
        this.priority = builder.priority;
        this.elevatorId = builder.elevatorId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int compareTo(ElevatorRequest other) {
        // Higher priority first, then by creation time (older first)
        int priorityComparison = Integer.compare(other.priority, this.priority);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        return this.createdAt.compareTo(other.createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElevatorRequest that = (ElevatorRequest) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ElevatorRequest{" +
                "id=" + id +
                ", requestedFloor=" + requestedFloor +
                ", type=" + type +
                ", direction=" + direction +
                ", elevatorId=" + (elevatorId < 0 ? "unassigned" : elevatorId) +
                ", priority=" + priority +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Builder for constructing ElevatorRequest instances.
     */
    public static class Builder {
        private int id;
        private int requestedFloor;
        private RequestType type;
        private Direction direction;
        private LocalDateTime createdAt;
        private int priority;
        private int elevatorId = -1; // Default: not yet assigned

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder requestedFloor(int requestedFloor) {
            this.requestedFloor = requestedFloor;
            return this;
        }

        public Builder type(RequestType type) {
            this.type = type;
            return this;
        }

        public Builder direction(Direction direction) {
            this.direction = direction;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder elevatorId(int elevatorId) {
            this.elevatorId = elevatorId;
            return this;
        }

        public ElevatorRequest build() {
            if (createdAt == null) {
                createdAt = LocalDateTime.now();
            }
            return new ElevatorRequest(this);
        }
    }
}
