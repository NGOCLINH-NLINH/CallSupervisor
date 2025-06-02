package org.example.calleventproducerservice;

import org.example.calleventproducerservice.model.MockAgentManager;
import org.example.calleventproducerservice.service.CallEventProducerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CallEventProducerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CallEventProducerServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner initSimulationData(MockAgentManager agentManager) {
		return args -> {
			agentManager.addAgents(CallEventProducerService.AGENT_IDS);
			agentManager.addQueues(CallEventProducerService.QUEUE_IDS);
			System.out.println("MockAgentManager initialized with agents and queues.");
		};
	}
}
