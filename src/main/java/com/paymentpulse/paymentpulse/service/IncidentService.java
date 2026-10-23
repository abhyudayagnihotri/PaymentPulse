package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.model.Incident;
import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.repository.IncidentRepository;
import com.paymentpulse.paymentpulse.repository.TransactionRepository;
import com.paymentpulse.paymentpulse.model.IncidentRcaEvidence;
import com.paymentpulse.paymentpulse.repository.IncidentRcaEvidenceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class IncidentService {

    private final AnomalyDetectionService anomalyDetectionService;
    private final IncidentRepository incidentRepository;
    private final TransactionRepository transactionRepository;
    private final GatewayReliabilityService gatewayReliabilityService;
    private final FailureCorrelationService failureCorrelationService;
    private final IncidentRcaEvidenceRepository rcaEvidenceRepository;

    public IncidentService(
            AnomalyDetectionService anomalyDetectionService,
            IncidentRepository incidentRepository,
            TransactionRepository transactionRepository,
            GatewayReliabilityService gatewayReliabilityService,
            FailureCorrelationService failureCorrelationService,
            IncidentRcaEvidenceRepository rcaEvidenceRepository) {

        this.anomalyDetectionService = anomalyDetectionService;
        this.incidentRepository = incidentRepository;
        this.transactionRepository = transactionRepository;
        this.gatewayReliabilityService=gatewayReliabilityService;
        this.failureCorrelationService=failureCorrelationService;
        this.rcaEvidenceRepository=rcaEvidenceRepository;
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
            return handleAnomaly(anomaly, windowMinutes, baselineWindows);
        }

        return handleRecovery();
    }

    private Map<String, Object> handleAnomaly(
            Map<String, Object> anomaly,
            int windowMinutes,
            int baselineWindows) {

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
                analyzeGateways(windowMinutes, baselineWindows);

        Map<String, Object> correlation = failureCorrelationService.analyze(windowMinutes);

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
            incident.setType(determineType((reasons)));
            incident.setHealthyEvaluations(0);

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
            saveRcaEvidence(incident,correlation);

            return buildIncidentResponse(incident,"Incident remains active",correlation);
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
        saveRcaEvidence(incident,correlation);

        return buildIncidentResponse(incident,"New incident detected",correlation);
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

        int healthyEvaluations =
                incident.getHealthyEvaluations() + 1;

        incident.setHealthyEvaluations(healthyEvaluations);

        if (healthyEvaluations < 3) {
            incidentRepository.save(incident);

            response.put("active", true);
            response.put("incidentId", incident.getId());
            response.put("status", "ACTIVE");
            response.put("healthyEvaluations", healthyEvaluations);
            response.put("requiredHealthyEvaluations", 3);
            response.put(
                    "message",
                    "Recovery pending; awaiting consecutive healthy evaluations");

            return response;
        }

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

        return buildIncidentResponse(activeIncident.get(),"Incident is active",failureCorrelationService.analyze(10));
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

    public List<IncidentRcaEvidence> getRcaHistory(String incidentId){
        return rcaEvidenceRepository.findByIncidentIdOrderByEvaluatedAtDesc(incidentId);
    }

    private GatewayAnalysis analyzeGateways(
            int windowMinutes,
            int baselineWindows) {

        List<Map<String, Object>> results =
                gatewayReliabilityService.analyzeGateways(
                        windowMinutes, baselineWindows);

        // Prefer gateways explicitly flagged as anomalous.
        Map<String, Object> selected = results.stream()
                .filter(g ->
                        Boolean.TRUE.equals(g.get("baselineAvailable")))
                .filter(g ->
                        Boolean.TRUE.equals(g.get("failureRateAnomaly"))
                                || Boolean.TRUE.equals(g.get("latencyAnomaly")))
                .max(Comparator.comparingDouble(g ->
                        ((Number) g.get("anomalyScore")).doubleValue()))
                .orElse(null);

        // If no gateway anomaly is identified, use the highest
        // current failure rate as a fallback candidate.
        if (selected == null) {
            selected = results.stream()
                    .filter(g ->
                            ((Number) g.get("currentTransactions"))
                                    .longValue() > 0)
                    .max(Comparator.comparingDouble(g ->
                            ((Number) g.get("currentFailureRate"))
                                    .doubleValue()))
                    .orElse(null);
        }

        if (selected == null) {
            return new GatewayAnalysis("UNKNOWN", 0.0);
        }

        return new GatewayAnalysis(
                String.valueOf(selected.get("gatewayId")),
                ((Number) selected.get("currentFailureRate"))
                        .doubleValue());
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
            String message,
            Map<String, Object> correlation) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("active", "ACTIVE".equals(incident.getStatus()));
        response.put("message", message);
        response.put("incidentId", incident.getId());
        response.put("severity", incident.getSeverity());
        response.put("status", incident.getStatus());
        response.put("type", incident.getType());
        response.put("startedAt", incident.getStartedAt());
        response.put("resolvedAt", incident.getResolvedAt());
        response.put("failureRate", incident.getFailureRate());
        response.put("p95Latency", incident.getP95Latency());
        response.put("suspectedGateway", incident.getGatewayId());
        response.put("gatewayFailureRate", incident.getGatewayFailureRate());
        response.put("description", incident.getDescription());

        // Attach correlation evidence.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pairs =
                (List<Map<String, Object>>) correlation.get("gatewayBankAnalysis");

        Map<String, Object> selectedPair = null;

        if (pairs != null) {
            selectedPair = pairs.stream()
                    .filter(p -> Boolean.TRUE.equals(p.get("suspiciousSignal")))
                    .filter(p -> Boolean.TRUE.equals(p.get("sufficientData")))
                    .filter(p -> Objects.equals(
                            String.valueOf(p.get("gatewayId")),
                            incident.getGatewayId()))
                    .findFirst()
                    .orElse(null);

            if (selectedPair == null) {
                selectedPair = pairs.stream()
                        .filter(p -> Boolean.TRUE.equals(p.get("sufficientData")))
                        .filter(p -> Objects.equals(
                                String.valueOf(p.get("gatewayId")),
                                incident.getGatewayId()))
                        .max(Comparator.comparingDouble(p ->
                                ((Number) p.get("failureRate")).doubleValue()))
                        .orElse(null);
            }
        }

        if (selectedPair != null) {
            double pairFailureRate =
                    ((Number) selectedPair.get("failureRate")).doubleValue();

            double overallFailureRate =
                    ((Number) correlation.get("overallFailureRate")).doubleValue();

            double failureRateGap = pairFailureRate - overallFailureRate;

            boolean suspicious = Boolean.TRUE.equals(selectedPair.get("suspiciousSignal"));

            response.put("suspectedBank", selectedPair.get("bankId"));
            response.put("pairFailureRate", pairFailureRate);
            response.put("overallFailureRate", overallFailureRate);
            response.put("failureRateGap", failureRateGap);
            response.put("pairTransactions", selectedPair.get("transactions"));
            response.put("pairFailures", selectedPair.get("failures"));
            response.put("confidence", suspicious? "MODERATE" : "LOW");
            response.put("bankAssessment", suspicious ? "SUSPICIOUS_SIGNAL" : "LEADING_CANDIDATE");
            response.put("evidence", List.of(
                    "Gateway-bank failure rate exceeds the overall window rate by "
                            + String.format("%.2f", failureRateGap)
                            + " percentage points",
                    "At least 10 transactions observed for this combination",
                    "Correlation is a suspicious signal, not proof of causation"
            ));
        } else {
            response.put("suspectedBank", null);
            response.put("confidence", "LOW");
            response.put("evidence", List.of(
                    "No eligible suspicious gateway-bank combination identified",
                    "The suspected gateway is based on gateway reliability analysis"
            ));
        }

        return response;
    }

    private void saveRcaEvidence(Incident incident, Map<String, Object> correlation){

        Map<String, Object> rca= buildIncidentResponse(incident, "RCA snapshot",correlation);
        IncidentRcaEvidence snapshot = new IncidentRcaEvidence();

        snapshot.setIncidentID(incident.getId());
        snapshot.setEvaluatedAt(LocalDateTime.now());
        snapshot.setSuspectedGateway(incident.getGatewayId());
        snapshot.setSuspectedBank((String) rca.get("suspectedBank"));

        Object pairRate = rca.get("pairFailureRate");
        if(pairRate instanceof Number n){
            snapshot.setPairFailureRate(n.doubleValue());
        }

        Object overallRate = rca.get("overallFailureRate");
        if(overallRate instanceof Number n){
            snapshot.setOverallFailureRate(n.doubleValue());
        }

        Object gap = rca.get("failureRateGap");
        if(gap instanceof Number n){
            snapshot.setFailureRateGap(n.doubleValue());
        }

        Object pairTransactions = rca.get("pairTransactions");
        if (pairTransactions instanceof Number n) {
            snapshot.setPairTransactions(n.intValue());
        }

        Object pairFailures = rca.get("pairFailures");
        if (pairFailures instanceof Number n) {
            snapshot.setPairFailures(n.intValue());
        }

        snapshot.setConfidence((String) rca.get("confidence"));

        Object evidence = rca.get("evidence");
        if(evidence instanceof List<?> items){
            snapshot.setEvidence(
                    items.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining("; "))
            );
        }
        rcaEvidenceRepository.save(snapshot);
    }

    private record GatewayAnalysis(
            String gatewayId,
            double failureRate
    ) {
    }
}