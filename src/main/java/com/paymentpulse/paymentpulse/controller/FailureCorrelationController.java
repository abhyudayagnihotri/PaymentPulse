package com.paymentpulse.paymentpulse.controller;

import com.paymentpulse.paymentpulse.service.FailureCorrelationService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/reliability")
public class FailureCorrelationController {

    private final FailureCorrelationService service;

    public FailureCorrelationController(FailureCorrelationService service){
        this.service=service;
    }

    @GetMapping("/correlation")
    public Map<String, Object> analyze(@RequestParam(defaultValue="10") int windowMinutes){
        return  service.analyze(windowMinutes);
    }

}
