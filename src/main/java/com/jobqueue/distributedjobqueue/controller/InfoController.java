package com.jobqueue.distributedjobqueue.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class InfoController {

    @GetMapping("/")
    public Map<String, Object> info() {

        Map<String, Object> response = new HashMap<>();

        response.put("project", "Distributed Job Queue System");
        response.put("status", "Running");

        response.put("features", List.of(
                "Asynchronous Job Processing",
                "Retry Mechanism",
                "Dead Letter Queue",
                "Redis-based Queue",
                "Docker Deployment"
        ));

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("submitJob", "POST /jobs");
        endpoints.put("getJobStatus", "GET /jobs/{id}");
        endpoints.put("health", "GET /health");

        response.put("endpoints", endpoints);

        return response;
    }
}