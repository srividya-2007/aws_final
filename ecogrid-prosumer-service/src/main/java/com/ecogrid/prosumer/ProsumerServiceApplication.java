package com.ecogrid.prosumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ProsumerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProsumerServiceApplication.class, args);
    }
}
