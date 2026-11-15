package com.abm.people.controllers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController 
@RequestMapping("/debug")
@ConditionalOnProperty(name = "app.debug-endpoints", havingValue = "true")
public class DebugController {
    private record SlowResponse(int ms) {} 

    @GetMapping("/slow")
    public SlowResponse slowEndpoint(@Min(0) @Max(10000) @RequestParam(defaultValue = "2000") int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new SlowResponse(ms);
    }

    @GetMapping("/error")
    public void errorEndpoint() {
        throw new IllegalStateException("Test exception");
    }
}
