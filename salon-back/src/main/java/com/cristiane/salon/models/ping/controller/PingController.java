package com.cristiane.salon.models.ping.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
public class PingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        log.info("Health check endpoint /ping requested. Status: OK");
        return Map.of(
            "status", "ok",
            "service", "eq03",
            "timestamp", java.time.Instant.now().toString()
        );
    }
}