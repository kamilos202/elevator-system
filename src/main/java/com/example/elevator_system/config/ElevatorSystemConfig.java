package com.example.elevator_system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * All the tuning knobs for the elevator system, bound from application.properties
 * under the "elevator.system" prefix. Sensible defaults are provided so the app
 * starts cleanly even without a properties file.
 *
 * The timing values (in milliseconds) are what make the simulation feel realistic —
 * bump them up if you want a lazier simulation, or shrink them for faster testing.
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "elevator.system")
public class ElevatorSystemConfig {
    private int numberOfElevators = 3;
    private int numberOfFloors = 10;
    private int elevatorCapacity = 10;

    /** How long it takes an elevator to travel one floor. */
    private long floorTransitionTimeMs = 1000;

    /** Time for the door to fully open or fully close (each way). */
    private long doorOpenCloseTimeMs = 2000;

    /** How long the doors stay open once they've finished opening. */
    private long doorStayOpenTimeMs = 3000;

    /** How often the simulation tick fires. 100 ms is fine for this scale. */
    private long elevatorTickIntervalMs = 100;

    @Override
    public String toString() {
        return "ElevatorSystemConfig{" +
                "numberOfElevators=" + numberOfElevators +
                ", numberOfFloors=" + numberOfFloors +
                ", elevatorCapacity=" + elevatorCapacity +
                ", floorTransitionTimeMs=" + floorTransitionTimeMs +
                ", doorOpenCloseTimeMs=" + doorOpenCloseTimeMs +
                ", doorStayOpenTimeMs=" + doorStayOpenTimeMs +
                ", elevatorTickIntervalMs=" + elevatorTickIntervalMs +
                '}';
    }
}
