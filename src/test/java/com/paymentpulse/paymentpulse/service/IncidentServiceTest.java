
package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.model.Incident;
import com.paymentpulse.paymentpulse.repository.IncidentRepository;
import com.paymentpulse.paymentpulse.repository.IncidentRcaEvidenceRepository;
import com.paymentpulse.paymentpulse.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private AnomalyDetectionService anomalyDetectionService;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private GatewayReliabilityService gatewayReliabilityService;

    @Mock
    private FailureCorrelationService failureCorrelationService;

    @Mock
    private IncidentRcaEvidenceRepository rcaEvidenceRepository;

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(
                anomalyDetectionService,
                incidentRepository,
                transactionRepository,
                gatewayReliabilityService,
                failureCorrelationService,
                rcaEvidenceRepository
        );
    }

    @Test
    void shouldReturnNoActiveIncidentWhenNoneExists() {
        when(anomalyDetectionService.detectAnomaly(10, 6))
                .thenReturn(Map.of("anomaly", false));

        when(incidentRepository
                .findFirstByStatusOrderByStartedAtDesc("ACTIVE"))
                .thenReturn(Optional.empty());

        Map<String, Object> response =
                incidentService.evaluate(10, 6);

        assertEquals(false, response.get("active"));
        assertEquals("No active incident", response.get("message"));
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void shouldIncrementHealthyEvaluationsOnRecovery() {
        Incident incident = activeIncident();

        when(anomalyDetectionService.detectAnomaly(10, 6))
                .thenReturn(Map.of("anomaly", false));

        when(incidentRepository
                .findFirstByStatusOrderByStartedAtDesc("ACTIVE"))
                .thenReturn(Optional.of(incident));

        Map<String, Object> response =
                incidentService.evaluate(10, 6);

        assertEquals(true, response.get("active"));
        assertEquals(1, response.get("healthyEvaluations"));
        assertEquals("ACTIVE", incident.getStatus());

        verify(incidentRepository).save(incident);
    }

    @Test
    void shouldResolveIncidentAfterThirdHealthyEvaluation() {
        Incident incident = activeIncident();
        incident.setHealthyEvaluations(2);

        when(anomalyDetectionService.detectAnomaly(10, 6))
                .thenReturn(Map.of("anomaly", false));

        when(incidentRepository
                .findFirstByStatusOrderByStartedAtDesc("ACTIVE"))
                .thenReturn(Optional.of(incident));

        Map<String, Object> response =
                incidentService.evaluate(10, 6);

        assertEquals(false, response.get("active"));
        assertEquals("RESOLVED", response.get("status"));
        assertEquals("RESOLVED", incident.getStatus());
        assertNotNull(incident.getResolvedAt());

        verify(incidentRepository).save(incident);
    }

    @Test
    void shouldResetRecoveryCounterWhenAnomalyReturns() {
        Incident incident = activeIncident();
        incident.setHealthyEvaluations(2);

        when(anomalyDetectionService.detectAnomaly(10, 6))
                .thenReturn(Map.of(
                        "anomaly", true,
                        "currentFailureRate", 40.0,
                        "currentP95Latency", 2000.0,
                        "severity", "HIGH",
                        "reasons", java.util.List.of(
                                "Failure rate exceeded baseline threshold"
                        )
                ));

        when(gatewayReliabilityService.analyzeGateways(10, 6))
                .thenReturn(java.util.List.of(
                        Map.of(
                                "gatewayId", "G03",
                                "currentTransactions", 20,
                                "currentFailureRate", 40.0,
                                "baselineAvailable", false,
                                "failureRateAnomaly", false,
                                "latencyAnomaly", false,
                                "anomalyScore", 0.0
                        )
                ));

        when(failureCorrelationService.analyze(10))
                .thenReturn(Map.of(
                        "overallFailureRate", 40.0,
                        "gatewayBankAnalysis", java.util.List.of()
                ));

        when(incidentRepository
                .findFirstByStatusOrderByStartedAtDesc("ACTIVE"))
                .thenReturn(Optional.of(incident));

        Map<String, Object> response =
                incidentService.evaluate(10, 6);

        assertEquals("ACTIVE", incident.getStatus());
        assertEquals(0, incident.getHealthyEvaluations());
        assertEquals("Incident remains active", response.get("message"));

        verify(incidentRepository).save(incident);
        verify(rcaEvidenceRepository).save(any());
    }


    @Test
    void shouldCreateNewIncidentWhenAnomalyIsDetected() {
        when(anomalyDetectionService.detectAnomaly(10, 6))
                .thenReturn(Map.of(
                        "anomaly", true,
                        "currentFailureRate", 40.0,
                        "currentP95Latency", 2500.0,
                        "severity", "HIGH",
                        "reasons", List.of(
                                "Failure rate exceeded baseline threshold"
                        )
                ));

        when(gatewayReliabilityService.analyzeGateways(10, 6))
                .thenReturn(List.of(
                        Map.of(
                                "gatewayId", "G03",
                                "currentTransactions", 20,
                                "currentFailureRate", 40.0,
                                "baselineAvailable", false,
                                "failureRateAnomaly", false,
                                "latencyAnomaly", false,
                                "anomalyScore", 0.0
                        )
                ));

        when(failureCorrelationService.analyze(10))
                .thenReturn(Map.of(
                        "overallFailureRate", 40.0,
                        "gatewayBankAnalysis", List.of()
                ));

        when(incidentRepository
                .findFirstByStatusOrderByStartedAtDesc("ACTIVE"))
                .thenReturn(Optional.empty());

        when(incidentRepository.count()).thenReturn(0L);

        Map<String, Object> response =
                incidentService.evaluate(10, 6);

        ArgumentCaptor<Incident> captor =
                ArgumentCaptor.forClass(Incident.class);

        verify(incidentRepository).save(captor.capture());
        verify(rcaEvidenceRepository).save(any());

        Incident savedIncident = captor.getValue();

        assertEquals("INC-0001", savedIncident.getId());
        assertEquals("ACTIVE", savedIncident.getStatus());
        assertEquals("HIGH", savedIncident.getSeverity());
        assertEquals("PAYMENT_FAILURE_SPIKE", savedIncident.getType());
        assertEquals("G03", savedIncident.getGatewayId());
        assertEquals(40.0, savedIncident.getFailureRate());
        assertEquals(true, response.get("active"));
        assertEquals("New incident detected", response.get("message"));
    }

    @Test
    void shouldUpdateExistingIncidentInsteadOfCreatingDuplicate() {
        Incident incident = activeIncident();

        when(anomalyDetectionService.detectAnomaly(10, 6))
                .thenReturn(Map.of(
                        "anomaly", true,
                        "currentFailureRate", 45.0,
                        "currentP95Latency", 3000.0,
                        "severity", "CRITICAL",
                        "reasons", List.of(
                                "Failure rate exceeded baseline threshold",
                                "P95 latency exceeded baseline threshold"
                        )
                ));

        when(gatewayReliabilityService.analyzeGateways(10, 6))
                .thenReturn(List.of(
                        Map.of(
                                "gatewayId", "G03",
                                "currentTransactions", 20,
                                "currentFailureRate", 45.0,
                                "baselineAvailable", false,
                                "failureRateAnomaly", false,
                                "latencyAnomaly", false,
                                "anomalyScore", 0.0
                        )
                ));

        when(failureCorrelationService.analyze(10))
                .thenReturn(Map.of(
                        "overallFailureRate", 45.0,
                        "gatewayBankAnalysis", List.of()
                ));

        when(incidentRepository
                .findFirstByStatusOrderByStartedAtDesc("ACTIVE"))
                .thenReturn(Optional.of(incident));

        Map<String, Object> response =
                incidentService.evaluate(10, 6);

        assertEquals("INC-0001", response.get("incidentId"));
        assertEquals("Incident remains active", response.get("message"));
        assertEquals("CRITICAL", incident.getSeverity());
        assertEquals(45.0, incident.getFailureRate());
        assertEquals(3000.0, incident.getP95Latency());
        assertEquals(0, incident.getHealthyEvaluations());

        verify(incidentRepository).save(incident);
        verify(incidentRepository, never()).count();
        verify(rcaEvidenceRepository).save(any());
    }


    @Test
    void shouldIdentifySuspiciousGatewayBankPair() {
        Incident incident = activeIncident();

        Map<String, Object> pair = new java.util.HashMap<>();
        pair.put("gatewayId", "G03");
        pair.put("bankId", "B15");
        pair.put("failureRate", 60.0);
        pair.put("transactions", 20);
        pair.put("failures", 12);
        pair.put("sufficientData", true);
        pair.put("suspiciousSignal", true);

        when(anomalyDetectionService.detectAnomaly(10, 6))
                .thenReturn(Map.of(
                        "anomaly", true,
                        "currentFailureRate", 40.0,
                        "currentP95Latency", 2500.0,
                        "severity", "HIGH",
                        "reasons", List.of(
                                "Failure rate exceeded baseline threshold"
                        )
                ));

        when(gatewayReliabilityService.analyzeGateways(10, 6))
                .thenReturn(List.of(
                        Map.of(
                                "gatewayId", "G03",
                                "currentTransactions", 20,
                                "currentFailureRate", 40.0,
                                "baselineAvailable", false,
                                "failureRateAnomaly", false,
                                "latencyAnomaly", false,
                                "anomalyScore", 0.0
                        )
                ));

        when(failureCorrelationService.analyze(10))
                .thenReturn(Map.of(
                        "overallFailureRate", 40.0,
                        "gatewayBankAnalysis", List.of(pair)
                ));

        when(incidentRepository
                .findFirstByStatusOrderByStartedAtDesc("ACTIVE"))
                .thenReturn(Optional.of(incident));

        Map<String, Object> response = incidentService.evaluate(10, 6);

        assertEquals("B15", response.get("suspectedBank"));
        assertEquals(60.0, response.get("pairFailureRate"));
        assertEquals(20, response.get("pairTransactions"));
        assertEquals(12, response.get("pairFailures"));
        assertEquals(20.0, response.get("failureRateGap"));
        assertEquals("MODERATE", response.get("confidence"));
        assertEquals("SUSPICIOUS_SIGNAL", response.get("bankAssessment"));

        verify(rcaEvidenceRepository).save(any());
    }

    @Test
    void shouldReturnLowConfidenceWhenNoPairHasEnoughData() {
        Incident incident = activeIncident();

        Map<String, Object> pair = new java.util.HashMap<>();
        pair.put("gatewayId", "G03");
        pair.put("bankId", "B15");
        pair.put("failureRate", 80.0);
        pair.put("transactions", 5);
        pair.put("failures", 4);
        pair.put("sufficientData", false);
        pair.put("suspiciousSignal", false);

        when(anomalyDetectionService.detectAnomaly(10, 6))
                .thenReturn(Map.of(
                        "anomaly", true,
                        "currentFailureRate", 40.0,
                        "currentP95Latency", 2500.0,
                        "severity", "HIGH",
                        "reasons", List.of(
                                "Failure rate exceeded baseline threshold"
                        )
                ));

        when(gatewayReliabilityService.analyzeGateways(10, 6))
                .thenReturn(List.of(
                        Map.of(
                                "gatewayId", "G03",
                                "currentTransactions", 20,
                                "currentFailureRate", 40.0,
                                "baselineAvailable", false,
                                "failureRateAnomaly", false,
                                "latencyAnomaly", false,
                                "anomalyScore", 0.0
                        )
                ));

        when(failureCorrelationService.analyze(10))
                .thenReturn(Map.of(
                        "overallFailureRate", 40.0,
                        "gatewayBankAnalysis", List.of(pair)
                ));

        when(incidentRepository
                .findFirstByStatusOrderByStartedAtDesc("ACTIVE"))
                .thenReturn(Optional.of(incident));

        Map<String, Object> response = incidentService.evaluate(10, 6);

        assertNull(response.get("suspectedBank"));
        assertEquals("LOW", response.get("confidence"));

        List<?> evidence = (List<?>) response.get("evidence");
        assertTrue(evidence.stream().anyMatch(
                item -> item.toString().contains("No eligible suspicious")
        ));

        verify(rcaEvidenceRepository).save(any());
    }

    private Incident activeIncident() {
        Incident incident = new Incident();
        incident.setId("INC-0001");
        incident.setStatus("ACTIVE");
        incident.setSeverity("HIGH");
        incident.setType("PAYMENT_DEGRADATION");
        incident.setStartedAt(LocalDateTime.now().minusMinutes(5));
        incident.setFailureRate(40.0);
        incident.setP95Latency(2000.0);
        incident.setGatewayId("G03");
        incident.setGatewayFailureRate(40.0);
        incident.setDescription("Test incident");
        incident.setHealthyEvaluations(0);
        return incident;
    }
}
