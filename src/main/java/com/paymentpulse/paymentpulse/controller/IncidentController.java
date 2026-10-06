package com.paymentpulse.paymentpulse.controller;

import com.paymentpulse.paymentpulse.model.Incident;
import com.paymentpulse.paymentpulse.service.IncidentService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService){
        this.incidentService=incidentService;
    }

    @PostMapping("/evaluate")
    public Map<String, Object> evaluate(@RequestParam(defaultValue = "10")int windowMinutes, @RequestParam(defaultValue="6")int windows){
        return incidentService.evaluate(windowMinutes, windows);
    }

    @GetMapping("/active")
    public Map<String, Object> getActiveIncident() {
        return incidentService.getActiveIncident();
    }

    @GetMapping
    public List<Incident> getAllIncidents(){
        return incidentService.getAllIncidents();
    }
}
