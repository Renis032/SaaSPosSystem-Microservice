package com.renko.controller;

import com.renko.service.DemoSeedService;
import com.renko.service.DevCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Profile("dev")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev")
public class DevCleanupController
{
    private final DevCleanupService devCleanupService;
    private final DemoSeedService demoSeedService;

    @DeleteMapping("/clear-db")
    public ResponseEntity<Map<String, Object>> clearDatabase()
    {
        List<String> truncatedTables = devCleanupService.clearAllTables();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Database cleared successfully");
        body.put("truncatedTables", truncatedTables);
        body.put("tableCount", truncatedTables.size());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/demo-info")
    public ResponseEntity<Map<String, Object>> demoInfo()
    {
        return ResponseEntity.ok(demoSeedService.demoInfo());
    }

    @PostMapping("/reset-demo")
    public ResponseEntity<Map<String, Object>> resetDemo()
    {
        return ResponseEntity.ok(demoSeedService.resetAndSeed());
    }
}
