package com.example.elevator_system.request;

import com.example.elevator_system.model.Direction;
import com.example.elevator_system.model.ElevatorRequest;
import com.example.elevator_system.model.RequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RequestQueue.
 *
 * The main things to verify: priority ordering, FIFO tie-breaking for equal priority,
 * and that getAllRequests gives a snapshot that doesn't affect the real queue.
 */
class RequestQueueTest {

    private RequestQueue queue;

    @BeforeEach
    void setUp() {
        queue = new RequestQueue();
    }

    private ElevatorRequest request(int id, int priority, LocalDateTime createdAt) {
        return ElevatorRequest.builder()
                .id(id)
                .requestedFloor(id)
                .type(RequestType.CALL)
                .direction(Direction.UP)
                .elevatorId(-1)
                .priority(priority)
                .createdAt(createdAt)
                .build();
    }

    @Test
    @DisplayName("fresh queue is empty")
    void emptyOnCreation() {
        assertThat(queue.isEmpty()).isTrue();
        assertThat(queue.size()).isZero();
    }

    @Test
    @DisplayName("poll on empty queue returns null without throwing")
    void pollReturnsNullWhenEmpty() {
        assertThat(queue.poll()).isNull();
    }

    @Test
    @DisplayName("higher-priority request is polled before a lower-priority one")
    void higherPriorityFirst() {
        queue.addRequest(request(1, 1, LocalDateTime.now()));
        queue.addRequest(request(2, 2, LocalDateTime.now().plusSeconds(1)));

        assertThat(queue.poll().getId()).isEqualTo(2); // priority 2 out first
        assertThat(queue.poll().getId()).isEqualTo(1); // priority 1 out second
    }

    @Test
    @DisplayName("for equal priority, the older request comes out first (FIFO)")
    void olderRequestFirstOnTie() {
        LocalDateTime earlier = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime later   = LocalDateTime.of(2024, 1, 1, 10, 0, 5);

        queue.addRequest(request(2, 1, later));   // added first but younger
        queue.addRequest(request(1, 1, earlier)); // added second but older

        assertThat(queue.poll().getId()).isEqualTo(1); // older wins
        assertThat(queue.poll().getId()).isEqualTo(2);
    }

    @Test
    @DisplayName("size reflects the actual number of queued items")
    void sizeReflectsCount() {
        queue.addRequest(request(1, 1, LocalDateTime.now()));
        queue.addRequest(request(2, 1, LocalDateTime.now()));
        assertThat(queue.size()).isEqualTo(2);

        queue.poll();
        assertThat(queue.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("clear removes everything")
    void clearEmptiesQueue() {
        queue.addRequest(request(1, 1, LocalDateTime.now()));
        queue.addRequest(request(2, 1, LocalDateTime.now()));
        queue.clear();
        assertThat(queue.isEmpty()).isTrue();
        assertThat(queue.size()).isZero();
    }

    @Test
    @DisplayName("getAllRequests returns a snapshot — clearing it doesn't affect the real queue")
    void getAllRequestsIsSnapshot() {
        queue.addRequest(request(1, 1, LocalDateTime.now()));
        queue.addRequest(request(2, 1, LocalDateTime.now()));

        List<ElevatorRequest> snapshot = queue.getAllRequests();
        assertThat(snapshot).hasSize(2);

        snapshot.clear(); // mess with the copy
        assertThat(queue.size()).isEqualTo(2); // real queue untouched
    }

    @Test
    @DisplayName("generateRequestId returns strictly increasing values")
    void generateRequestIdIsMonotonic() {
        int a = queue.generateRequestId();
        int b = queue.generateRequestId();
        int c = queue.generateRequestId();
        assertThat(a).isLessThan(b).isLessThan(c);
    }

    @Test
    @DisplayName("isEmpty becomes false after addRequest and true again after poll drains the queue")
    void isEmptyTracksState() {
        assertThat(queue.isEmpty()).isTrue();
        queue.addRequest(request(1, 1, LocalDateTime.now()));
        assertThat(queue.isEmpty()).isFalse();
        queue.poll();
        assertThat(queue.isEmpty()).isTrue();
    }
}

