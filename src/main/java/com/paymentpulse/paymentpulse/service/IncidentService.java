package com.paymentpulse.paymentpulse.service;

import org.springframework.stereotype.Service;
import com.paymentpulse.paymentpulse.model.Incident;
import java.util.*;
import java.time.LocalDateTime;
@Service
public class IncidentService {

    private final AnomalyDetectionService anomalyDetectionService;
    private final Map<String, Incident> incidents = new LinkedHashMap<>();
    private String activeIncidentId=null;

    public IncidentService(AnomalyDetectionService anomalyDetectionService){
        this.anomalyDetectionService=anomalyDetectionService;
    }

    public synchronized Map<String, Object> evaluate(int windowMinutes, int baselineWindows){

        Map<String, Object> anomaly = anomalyDetectionService.detectAnomaly(windowMinutes,baselineWindows);

        boolean isAnomaly;
        if(anomaly.get("anomaly").equals(true))
            return handleAnomaly(anomaly);
        else
            return handleRecovery();
    }

    private Map<String, Object> handleAnomaly(Map<String, Object> anomaly){
        LocalDateTime now = LocalDateTime.now();

        String severity = String.valueOf(anomaly.get("severity")).toUpperCase();

        double failureRate = ((Number) anomaly.get("currentFailureRate")).doubleValue();

        double p95Latency = ((Number) anomaly.get("currentP95Latency")).doubleValue();

        @SuppressWarnings("unchecked")
        List<String> reasons = (List<String>) anomaly.get("reasons");

        //Existing active incident
        if(activeIncidentId!=null){
            Incident existing = incidents.get(activeIncidentId);

            if(existing != null){

                existing.setSeverity(severity);
                existing.setFailureRate(failureRate);
                existing.setP95Latency(p95Latency);
                existing.setDescription(buildDescription(reasons));

                return buildIncidentResponse(existing,"Incident remains active");
            }
        }

        String incidentId="INC-" + String.format("%04d", incidents.size()+1);

        Incident incident = new Incident(incidentId, severity, "ACTIVE", determineType(reasons), now, failureRate, p95Latency, buildDescription(reasons));

        incidents.put(incidentId, incident);

        activeIncidentId = incidentId;

        return buildIncidentResponse(incident,"New incident detected");
    }

    private Map<String, Object> handleRecovery() {
        Map<String, Object> response = new LinkedHashMap<>();

        if(activeIncidentId==null){
            response.put("active",false);
            response.put("message","No active incident");

            return response;
        }

        Incident incident = incidents.get(activeIncidentId);

        if(incident!=null){
            incident.setStatus("RESOLVED");
            incident.setResolvedAt(LocalDateTime.now());
        }

        response.put("active", false);
        response.put("incidentId",activeIncidentId);
        response.put("status","RESOLVED");
        response.put("resolvedAt", incident != null ? incident.getResolvedAt() : null);

        activeIncidentId=null;
        return response;
    }

    public Map<String, Object> getActiveIncident() {
        Map<String, Object> response = new LinkedHashMap<>();

        if(activeIncidentId == null){
            response.put("active", false);
            response.put("message","No active incident");

            return response;
        }

        Incident incident = incidents.get(activeIncidentId);

        if(incident == null){
            response.put("active",false);
            return response;
        }

        return buildIncidentResponse(incident, "Incident is active");
    }

    public List<Incident> getAllIncidents(){
        return new ArrayList<>(incidents.values());
    }

    private String determineType(List<String> reasons){
        boolean failure = reasons.stream()
                .anyMatch(r-> r.toLowerCase().contains("failure"));

        boolean latency= reasons.stream()
                .anyMatch(r-> r.toLowerCase().contains("latency"));

        if(failure && latency) {
            return "PAYMENT_DEGRADATION";
        }

        if(failure){
            return "PAYMENT FAILURE SPIKE";
        }

        if(latency){
            return "LATENCY_DEGRADATION";
        }
        return "UNKNOWN";
    }

    private String buildDescription(List<String> reasons){
        return String.join("; ", reasons);
    }

    private Map<String, Object> buildIncidentResponse(Incident incident, String message){
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("active", "ACTIVE".equals(incident.getStatus()));
        response.put("message", message);
        response.put("incidentId", incident.getId());
        response.put("severity", incident.getSeverity());
        response.put("status", incident.getStatus());
        response.put("type",incident.getType());
        response.put("startedAt",incident.getStartedAt());
        response.put("resolvedAt", incident.getResolvedAt());
        response.put("failureRate",incident.getFailureRate());
        response.put("p95Latency",incident.getP95Latency());
        response.put("description", incident.getDescription());

        return response;
    }
}
