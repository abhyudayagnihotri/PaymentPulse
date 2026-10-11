package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
@Service
public class GatewayReliabilityService {
    private final TransactionRepository repository;

    public GatewayReliabilityService(TransactionRepository repository){
        this.repository=repository;
    }

    public List<Map<String, Object>> analyzeGateways(int windowMinutes, int baselineWindows){

        if(windowMinutes <=0 || baselineWindows <= 0){
            throw new IllegalArgumentException("windowMinutes and baselineWindows must be greater than 0");
        }
        LocalDateTime now = LocalDateTime.now();

        List<Transaction> transactions = repository.findAll().stream()
                .filter(t -> t.getTimestamp()!=null)
                .toList();

        List<Map<String, Object>> results = new ArrayList<>();

        Set<String> gateways = new HashSet<>();

        for(Transaction transaction: transactions){
            String gateway = transaction.getGatewayId();

            if(gateway == null || gateway.isBlank()) {
                gateway = "UNKNOWN";
            }
            gateways.add(gateway);
        }

        for(String gateway : gateways) {
            GatewayStats current = calculateStats(transactions, gateway, now.minusMinutes(windowMinutes), now);

            List<Double> historicalFailureRates = new ArrayList<>();

            List<Double> historicalLatencies = new ArrayList<>();

            for (int i = 1; i <= baselineWindows; i++) {
                LocalDateTime end = now.minusMinutes((long) windowMinutes * i);


                LocalDateTime start = end.minusMinutes(windowMinutes);

                GatewayStats stats = calculateStats(transactions, gateway, start, end);

                if (stats.transactions > 0) {
                    historicalFailureRates.add(stats.failureRate);
                    historicalLatencies.add(stats.averageLatency);
                }
            }
            double baselineFailureRate = average(historicalFailureRates);

            double baselineLatency = average(historicalLatencies);

            double failureRateStdDev = standardDeviation(historicalFailureRates);

            double latencyStdDev = standardDeviation(historicalLatencies);

            int availableBaselineWindows= historicalFailureRates.size();

            boolean baselineAvailable = availableBaselineWindows >=3;

            double failureThreshold = baselineFailureRate + 3 * failureRateStdDev;

            double latencyThreshold = baselineLatency + 3 * latencyStdDev;

            boolean failureAnomaly = baselineAvailable && current.transactions > 0 && current.failureRate > failureThreshold;

            boolean latencyAnomaly = baselineAvailable && current.transactions > 0 && current.averageLatency > latencyThreshold;

            double anomalyScore = 0;

            if (baselineAvailable && current.transactions > 0) {

                double failureExcess =
                        current.failureRate - failureThreshold;

                if (failureRateStdDev > 0) {
                    anomalyScore += Math.max(
                            0, failureExcess / failureRateStdDev);
                } else if (failureAnomaly) {
                    anomalyScore += 3;
                }

                double latencyExcess =
                        current.averageLatency - latencyThreshold;

                if (latencyStdDev > 0) {
                    anomalyScore += Math.max(
                            0, latencyExcess / latencyStdDev);
                } else if (latencyAnomaly) {
                    anomalyScore += 3;
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("gatewayId", gateway);
            result.put("currentTransactions", current.transactions);
            result.put("currentFailureRate", current.failureRate);
            result.put("baselineFailureRate", baselineFailureRate);
            result.put("failureRateThreshold", failureThreshold);
            result.put("currentAverageLatency", current.averageLatency);
            result.put("baselineAverageLatency", baselineLatency);
            result.put("latencyThreshold", latencyThreshold);
            result.put("failureRateStdDev", failureRateStdDev);
            result.put("latencyStdDev", latencyStdDev);
            result.put("failureRateAnomaly", failureAnomaly);
            result.put("latencyAnomaly", latencyAnomaly);
            result.put("anomalyScore", anomalyScore);
            result.put("availableBaselineWindows",availableBaselineWindows);
            result.put("baselineAvailable",baselineAvailable);
            results.add(result);
        }
        results.sort((a,b) -> Double.compare(((Number)b.get("anomalyScore")).doubleValue(), ((Number)a.get("anomalyScore")).doubleValue()));
        return results;
    }

    private GatewayStats calculateStats(List<Transaction> transactions, String gateway, LocalDateTime start, LocalDateTime end){

        List<Transaction> filtered = transactions.stream()
                .filter(t-> {
                    String id=t.getGatewayId();
                    if(id ==null || id.isBlank()){
                        id="UNKNOWN";
                    }
                    return id.equals(gateway);
                })
                .filter(t -> !t.getTimestamp().isBefore(start))
                .filter(t-> t.getTimestamp().isBefore(end))
                .toList();

        if(filtered.isEmpty()) {
            return new GatewayStats(0,0,0);
        }
        long failures = filtered.stream()
                .filter(t->t.getStatus().equalsIgnoreCase(("FAILED")))
                .count();

        double failureRate = failures * 100.0 / filtered.size();

        double averageLatency = filtered.stream()
                .filter(t -> t.getLatencyMs() != null)
                .mapToInt(Transaction::getLatencyMs)
                .average()
                .orElse(0);

        return new GatewayStats(filtered.size(),failureRate,averageLatency);
    }

    private double average(List<Double> values) {

        if (values.isEmpty()) {
            return 0;
        }

        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }

    private double standardDeviation(List<Double> values) {

        if (values.size() <= 1) {
            return 0;
        }

        double mean =
                average(values);

        double variance =
                values.stream()
                        .mapToDouble(
                                value ->
                                        Math.pow(
                                                value - mean,
                                                2
                                        )
                        )
                        .average()
                        .orElse(0);

        return Math.sqrt(variance);
    }

    private record GatewayStats(
            long transactions,
            double failureRate,
            double averageLatency) {
    }
}
