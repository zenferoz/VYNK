package com.vynk.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping({"/api/health", "/health"})
    public String health() {
        return "VYNK backend is running!";
    }
}