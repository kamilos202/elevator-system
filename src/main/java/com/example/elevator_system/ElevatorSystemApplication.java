package com.example.elevator_system;

import com.example.elevator_system.websocket.ElevatorEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ElevatorSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ElevatorSystemApplication.class, args);
	}

	@Bean
	public CommandLineRunner init(ElevatorEventPublisher publisher) {
		return args -> publisher.startPublishing();
	}
}
