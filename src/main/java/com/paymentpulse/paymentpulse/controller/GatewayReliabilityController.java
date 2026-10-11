package com.paymentpulse.paymentpulse.controller;

import com.paymentpulse.paymentpulse.service.GatewayReliabilityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class GatewayReliabilityController {

    private final GatewayReliabilityService gatewayService;

    public GatewayReliabilityController(GatewayReliabilityService gatewayService){
        this.gatewayService=gatewayService;
    }

    @GetMapping("/api/gateways/reliability")
    public List<Map<String, Object>> getGatewayReliability(@RequestParam(defaultValue = "10")int windowMinutes, @RequestParam(defaultValue = "6") int windows){
        return gatewayService.analyzeGateways(windowMinutes, windows);
    }
}
