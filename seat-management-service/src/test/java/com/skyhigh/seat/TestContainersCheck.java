package com.skyhigh.seat;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

public class TestContainersCheck {
    public static void main(String[] args) {
        System.out.println("Starting TestContainers Check...");
        try {
            System.out.println("Starting Redis...");
            GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                    .withExposedPorts(6379);
            redis.start();
            System.out.println("Redis Started on port: " + redis.getFirstMappedPort());
            redis.stop();

            System.out.println("Starting RabbitMQ...");
            RabbitMQContainer rabbitMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.12-management"));
            rabbitMQ.start();
            System.out.println("RabbitMQ Started on port: " + rabbitMQ.getAmqpPort());
            rabbitMQ.stop();

            System.out.println("SUCCESS: Containers started.");
        } catch (Throwable e) {
            System.err.println("FAILURE: Container startup failed.");
            e.printStackTrace();
        }
    }
}
