package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.model.Incident;
import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.repository.IncidentRepository;
import com.paymentpulse.paymentpulse.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class IncidentService {

    private final AnomalyDetectionService anomalyDetectionService;
    private final IncidentRepository incidentRepository;
    private final TransactionRepository transactionRepository;

    public IncidentService(
            AnomalyDetectionService anomalyDetectionService,
            IncidentRepository incidentRepository,
            TransactionRepository transactionRepository) {

        this.anomalyDetectionService =
                anomalyDetectionService;

        this.incidentRepository =
                incidentRepository;

        this.transactionRepository =
                transactionRepository;
    }

    public synchronized Map<String, Object> evaluate(
            int windowMinutes,
            int baselineWindows) {

        Map<String, Object> anomaly =
                anomalyDetectionService.detectAnomaly(
                        windowMinutes,
                        baselineWindows
                );

        boolean isAnomaly =
                Boolean.TRUE.equals(
                        anomaly.get("anomaly")
                );

        if (isAnomaly) {
            return handleAnomaly(
                    anomaly,
                    windowMinutes
            );
        }

        return handleRecovery();
    }

    private Map<String, Object> handleAnomaly(
            Map<String, Object> anomaly,
            int windowMinutes) {

        LocalDateTime now =
                LocalDateTime.now();

        double failureRate =
                ((Number) anomaly.get(
                        "currentFailureRate"
                )).doubleValue();

        double p95Latency =
                ((Number) anomaly.get(
                        "currentP95Latency"
                )).doubleValue();

        String severity =
                String.valueOf(
                        anomaly.get("severity")
                ).toUpperCase();

        @SuppressWarnings("unchecked")
        List<String> reasons =
                (List<String>) anomaly.get("reasons");

        /*
         * Find the gateway contributing most to
         * the current failure rate.
         */
        GatewayAnalysis gateway =
                analyzeGateways(windowMinutes);

        Optional<Incident> activeIncident =
                incidentRepository
                        .findFirstByStatusOrderByStartedAtDesc(
                                "ACTIVE"
                        );

        if (activeIncident.isPresent()) {

            Incident incident =
                    activeIncident.get();

            incident.setSeverity(severity);
            incident.setFailureRate(failureRate);
            incident.setP95Latency(p95Latency);

            incident.setGatewayId(
                    gateway.gatewayId()
            );

            incident.setGatewayFailureRate(
                    gateway.failureRate()
            );

            incident.setDescription(
                    buildDescription(
                            reasons,
                            gateway
                    )
            );

            incidentRepository.save(incident);

            return buildIncidentResponse(
                    incident,
                    "Incident remains active"
            );
        }

        String incidentId =
                generateIncidentId();

        Incident incident =
                new Incident();

        incident.setId(incidentId);
        incident.setSeverity(severity);
        incident.setStatus("ACTIVE");
        incident.setType(
                determineType(reasons)
        );
        incident.setStartedAt(now);
        incident.setFailureRate(failureRate);
        incident.setP95Latency(p95Latency);

        incident.setGatewayId(
                gateway.gatewayId()
        );

        incident.setGatewayFailureRate(
                gateway.failureRate()
        );

        incident.setDescription(
                buildDescription(
                        reasons,
                        gateway
                )
        );

        incidentRepository.save(incident);

        return buildIncidentResponse(
                incident,
                "New incident detected"
        );
    }

    private Map<String, Object> handleRecovery() {

        Optional<Incident> activeIncident =
                incidentRepository
                        .findFirstByStatusOrderByStartedAtDesc(
                                "ACTIVE"
                        );

        Map<String, Object> response =
                new LinkedHashMap<>();

        if (activeIncident.isEmpty()) {

            response.put("active", false);
            response.put(
                    "message",
                    "No active incident"
            );

            return response;
        }

        Incident incident =
                activeIncident.get();

        incident.setStatus("RESOLVED");
        incident.setResolvedAt(
                LocalDateTime.now()
        );

        incidentRepository.save(incident);

        response.put("active", false);
        response.put(
                "incidentId",
                incident.getId()
        );
        response.put(
                "status",
                "RESOLVED"
        );
        response.put(
                "resolvedAt",
                incident.getResolvedAt()
        );

        return response;
    }

    public Map<String, Object> getActiveIncident() {

        Optional<Incident> activeIncident =
                incidentRepository
                        .findFirstByStatusOrderByStartedAtDesc(
                                "ACTIVE"
                        );

        if (activeIncident.isEmpty()) {

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("active", false);
            response.put(
                    "message",
                    "No active incident"
            );

            return response;
        }

        return buildIncidentResponse(
                activeIncident.get(),
                "Incident is active"
        );
    }

    public List<Incident> getAllIncidents() {

        return incidentRepository.findAll();
    }

    private String generateIncidentId() {

        long count =
                incidentRepository.count() + 1;

        return String.format(
                "INC-%04d",
                count
        );
    }

    private GatewayAnalysis analyzeGateways(
            int windowMinutes) {

        LocalDateTime end =
                LocalDateTime.now();

        LocalDateTime start =
                end.minusMinutes(windowMinutes);

        List<Transaction> transactions =
                transactionRepository.findAll()
                        .stream()
                        .filter(t ->
                                t.getTimestamp() != null)
                        .filter(t ->
                                !t.getTimestamp()
                                        .isBefore(start))
                        .filter(t ->
                                t.getTimestamp()
                                        .isBefore(end))
                        .toList();

        Map<String, List<Transaction>> grouped =
                new HashMap<>();

        for (Transaction transaction : transactions) {

            String gateway =
                    transaction.getGatewayId();

            if (gateway == null ||
                    gateway.isBlank()) {

                gateway = "UNKNOWN";
            }

            grouped
                    .computeIfAbsent(
                            gateway,
                            k -> new ArrayList<>())
                    .add(transaction);
        }

        String worstGateway = "UNKNOWN";
        double worstFailureRate = 0;

        for (Map.Entry<String, List<Transaction>> entry
                : grouped.entrySet()) {

            List<Transaction> gatewayTransactions =
                    entry.getValue();

            long failures =
                    gatewayTransactions.stream()
                            .filter(t ->
                                    "FAILED".equalsIgnoreCase(
                                            t.getStatus()))
                            .count();

            double failureRate =
                    failures * 100.0
                            / gatewayTransactions.size();

            if (failureRate > worstFailureRate) {

                worstFailureRate =
                        failureRate;

                worstGateway =
                        entry.getKey();
            }
        }

        return new GatewayAnalysis(
                worstGateway,
                worstFailureRate
        );
    }

    private String determineType(
            List<String> reasons) {

        boolean failure =
                reasons.stream()
                        .anyMatch(r ->
                                r.toLowerCase()
                                        .contains("failure"));

        boolean latency =
                reasons.stream()
                        .anyMatch(r ->
                                r.toLowerCase()
                                        .contains("latency"));

        if (failure && latency) {
            return "PAYMENT_DEGRADATION";
        }

        if (failure) {
            return "PAYMENT_FAILURE_SPIKE";
        }

        if (latency) {
            return "LATENCY_DEGRADATION";
        }

        return "UNKNOWN";
    }

    private String buildDescription(
            List<String> reasons,
            GatewayAnalysis gateway) {

        String reasonText =
                String.join("; ", reasons);

        return reasonText
                + "; Suspected gateway: "
                + gateway.gatewayId()
                + " with "
                + String.format(
                "%.2f",
                gateway.failureRate())
                + "% failure rate";
    }

    private Map<String, Object> buildIncidentResponse(
            Incident incident,
            String message) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "active",
                "ACTIVE".equals(
                        incident.getStatus())
        );

        response.put("message", message);
        response.put(
                "incidentId",
                incident.getId()
        );
        response.put(
                "severity",
                incident.getSeverity()
        );
        response.put(
                "status",
                incident.getStatus()
        );
        response.put(
                "type",
                incident.getType()
        );
        response.put(
                "startedAt",
                incident.getStartedAt()
        );
        response.put(
                "resolvedAt",
                incident.getResolvedAt()
        );
        response.put(
                "failureRate",
                incident.getFailureRate()
        );
        response.put(
                "p95Latency",
                incident.getP95Latency()
        );
        response.put(
                "suspectedGateway",
                incident.getGatewayId()
        );
        response.put(
                "gatewayFailureRate",
                incident.getGatewayFailureRate()
        );
        response.put(
                "description",
                incident.getDescription()
        );

        return response;
    }

    private record GatewayAnalysis(
            String gatewayId,
            double failureRate
    ) {
    }
}