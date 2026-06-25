package com.cristiane.salon.models.maintenance.controller;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sysadmin/maintenance")
public class DatabaseMaintenanceController {

    private final Flyway flyway;

    public DatabaseMaintenanceController(ObjectProvider<Flyway> flywayProvider) {
        this.flyway = flywayProvider.getIfAvailable();
    }

    @PostMapping("/reset-schema")
    @PreAuthorize("hasRole('SYSADMIN')")
    public ResponseEntity<String> resetSchema() {
        if (flyway == null) {
            return ResponseEntity.badRequest().body("Flyway não está disponível neste contexto.");
        }
        flyway.clean();
        flyway.migrate();
        return ResponseEntity.ok("Schema do banco de dados resetado e atualizado com sucesso.");
    }
}
