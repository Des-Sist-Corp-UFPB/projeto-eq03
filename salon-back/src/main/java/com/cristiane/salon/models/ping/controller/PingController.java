package com.cristiane.salon.models.ping.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PingController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Monitorado por ferramentas externas (ex.: Uptime Kuma). Além de confirmar que a API
     * está de pé, executa um SELECT 1 para validar a conexão com o banco: se o banco cair,
     * o endpoint responde 503 em vez de 200, para que a queda seja detectada de verdade.
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        boolean databaseUp = isDatabaseUp();

        Map<String, String> body = new LinkedHashMap<>();
        body.put("service", "eq03");
        body.put("timestamp", Instant.now().toString());
        body.put("database", databaseUp ? "up" : "down");
        body.put("status", databaseUp ? "ok" : "error");

        if (databaseUp) {
            log.info("Health check endpoint /ping requested. Status: OK");
            return ResponseEntity.ok(body);
        }

        log.error("Health check endpoint /ping requested. Status: DATABASE DOWN");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean isDatabaseUp() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(result);
        } catch (DataAccessException ex) {
            log.error("Database health check failed during /ping: {}", ex.getMessage());
            return false;
        }
    }
}