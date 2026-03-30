package com.example.elevator_system.websocket;

import com.example.elevator_system.dto.ElevatorStatusDTO;
import com.example.elevator_system.service.ElevatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Pushes elevator state to all connected browser clients via STOMP/WebSocket.
 *
 * There are two update pipelines in this app:
 *  - HTTP polling: Angular calls GET /api/elevator/status every 500 ms for a full
 *    system snapshot. Used mainly on first load and as a fallback if WebSocket drops.
 *  - WebSocket push (this class): broadcasts each elevator's status to /topic/elevators
 *    every 500 ms without the client having to ask. This is what drives the live UI.
 *
 * 500 ms is a reasonable interval — fast enough to look smooth, slow enough that
 * we're not hammering the thread scheduler for no reason.
 */
@Component
public class ElevatorEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(ElevatorEventPublisher.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ElevatorService elevatorService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1,
            r -> {
                Thread t = new Thread(r, "WebSocket-Publisher");
                t.setDaemon(true);
                return t;
            });

    public ElevatorEventPublisher() {
    }

    /**
     * Starts the background publishing loop. Called once at application startup
     * from ElevatorSystemApplication — we don't want it firing before the service
     * is fully initalised.
     */
    public void startPublishing() {
        logger.info("Starting WebSocket event publisher");
        scheduler.scheduleAtFixedRate(
                this::publishElevatorUpdates,
                0,
                500, // send updates every 500 ms
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Grabs the current status of every elevator and fires one STOMP message per
     * elevator to /topic/elevators. The Angular client has a subscription on that
     * topic and updates the relevant elevator in its local state when it receives one.
     */
    private void publishElevatorUpdates() {
        try {
            List<ElevatorStatusDTO> statuses = elevatorService.getAllElevatorStatus();
            for (ElevatorStatusDTO status : statuses) {
                ElevatorEventMessage event = new ElevatorEventMessage(
                        "ELEVATOR_STATUS_UPDATE",
                        status,
                        "Elevator " + status.getId() + " at floor " + status.getCurrentFloor()
                );
                messagingTemplate.convertAndSend("/topic/elevators", event);
            }
        } catch (Exception e) {
            logger.error("Error publishing elevator updates", e);
        }
    }

    /**
     * One-off publish for a specific event (e.g. door open/close).
     * Not currently wired to any callers but useful if you want to push
     * targeted notifications rather than the blanket status updates.
     */
    public void publishEvent(ElevatorEventMessage event) {
        try {
            messagingTemplate.convertAndSend("/topic/elevators", event);
        } catch (Exception e) {
            logger.error("Error publishing event", e);
        }
    }

    /**
     * Shuts down the publisher.
     */
    public void shutdown() {
        logger.info("Shutting down WebSocket publisher");
        scheduler.shutdown();
    }
}
