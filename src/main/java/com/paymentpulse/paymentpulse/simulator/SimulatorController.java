package com.paymentpulse.paymentpulse.simulator;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final TransactionSimulator simulator;

    public SimulatorController(TransactionSimulator simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestParam(defaultValue = "1") int count){

        int generated = simulator.generateBatch(count).size();

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("message","Transaction generated successfully");

        response.put("generated",generated);

        return response;
    }

    @PostMapping("/incident")
    public Map<String, Object>  generateIncident(@RequestParam(defaultValue = "G01")String gatewayId, @RequestParam(defaultValue = "100")int count){

        int generated = simulator.generateGatewayIncident(gatewayId, count).size();

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("message", "Gateway Incident generated");
        response.put("gatewayId",gatewayId);
        response.put("generated",generated);
        response.put("simulatedFailureRate",40);
        response.put("incidentLatencyRange","1500-5000 ms");

        return response;
    }
}
