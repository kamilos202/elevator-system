package com.example.elevator_system.request;

import com.example.elevator_system.model.ElevatorRequest;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe priority queue for pending elevator requests.
 *
 * Requests are ordered by priority (higher first), with ties broken by creation time
 * (oldest first). The typical usage pattern is pretty straightforward:
 *
 * <pre>
 *   // Producer (HTTP thread): add a new request
 *   queue.addRequest(request);
 *
 *   // Consumer (simulation tick thread): drain all waiting requests
 *   while (!queue.isEmpty()) {
 *       ElevatorRequest r = queue.poll();
 *       // assign r to the best elevator
 *   }
 * </pre>
 *
 * In practice the queue is nearly always empty — requests arrive via HTTP and get
 * dispatched on the very next tick (100 ms later), so they don't pile up.
 */
public class RequestQueue {
    private final PriorityQueue<ElevatorRequest> queue;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private int requestIdCounter = 0;

    public RequestQueue() {
        this.queue = new PriorityQueue<>();
    }

    /**
     * Adds a new request. Safe to call from any thread.
     */
    public void addRequest(ElevatorRequest request) {
        lock.writeLock().lock();
        try {
            queue.offer(request);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves and removes the highest-priority request, or {@code null} if empty.
     */
    public ElevatorRequest poll() {
        lock.writeLock().lock();
        try {
            return queue.poll();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * How many requests are currently waiting. Mostly used for debug logging.
     */
    public int size() {
        lock.readLock().lock();
        try {
            return queue.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns true if there's nothing in the queue.
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Drops all queued requests — called on shutdown so nothing fires after the
     * simulation stops.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            queue.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns a snapshot of everything in the queue at this moment.
     * The list is a copy, so modifying it won't affect the real queue.
     */
    public List<ElevatorRequest> getAllRequests() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(queue);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Monotonically increasing ID generator. Each request gets a unique ID
     * so we can tell them apart in logs.
     */
    public synchronized int generateRequestId() {
        return ++requestIdCounter;
    }
}
