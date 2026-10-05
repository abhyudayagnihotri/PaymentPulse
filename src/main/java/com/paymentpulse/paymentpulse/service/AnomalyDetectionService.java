package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.repository.TransactionRepository;
import jdk.jshell.execution.LoaderDelegate;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AnomalyDetectionService {

    private final TransactionRepository repository;
    private final ReliabilityBaselineService baselineService;

    public AnomalyDetectionService(TransactionRepository repository, ReliabilityBaselineService baselineService){
        this.repository = repository;
        this.baselineService = baselineService;
    }

    public Map<String, Object> detectAnomaly(int windowMinutes, int baselineWindows){
        Map<String, Object>baseline = baselineService.getBaseline(windowMinutes, baselineWindows);

        double baselineFailureRate = ((Number) baseline.get("baselineFailureRate")).doubleValue();

        double failureRateStdDev = ((Number)baseline.get("failureRateStdDev")).doubleValue();

        double baselineP95 = ((Number)baseline.get("baselineP95Latency")).doubleValue();

        double latencyStdDev = ((Number)baseline.get("latencyStdDev")).doubleValue();

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMinutes((windowMinutes));

        List<Transaction> transactions = repository.findAll().stream()
                .filter(t-> t.getTimestamp()!=null)
                .filter(t-> t.getTimestamp().isBefore(end) && !t.getTimestamp().isBefore(start))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();

        if(transactions.isEmpty()) {
            result.put("anomaly",false);
            result.put("severity", "UNKNOWN");
            result.put("message" , "No transactions found in the current window");
            return result;
        }

        //current failure rate

        long total = transactions.size();

        long failures = transactions.stream()
                .filter(t -> t.getStatus().equalsIgnoreCase(("FAILED")))
                .count();

        double currentFailureRate = failures * 100.0 / total;

        //current P95 Latency
        List<Integer> latencies = transactions.stream()
                .map(Transaction::getLatencyMs)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        double currentP95 = calculatePercentile(latencies, 95);

        // calculate thresholds

        double failureRateThreshold = baselineFailureRate + 3 * failureRateStdDev;

        double latencyThreshold = baselineP95 + 3 * latencyStdDev;

        // Detect anomalies
        boolean failureRateAnomaly = currentFailureRate > failureRateThreshold;

        boolean latencyAnomaly = currentP95 > latencyThreshold;

        boolean anomaly = failureRateAnomaly || latencyAnomaly;

        // determining severity

        String severity = "NORMAL";

        if(failureRateAnomaly && latencyAnomaly)
            severity = "CRITICAL";

        if(failureRateAnomaly || latencyAnomaly)
            severity = "HIGH";

        // Reasons

        List<String> reasons = new ArrayList<>();

        if(failureRateAnomaly)
            reasons.add("Failure rate exceeded baseline threshold");

        if(latencyAnomaly)
            reasons.add("P95 latency exceeded baseline threshold");

        //Response

        result.put("anomaly",anomaly);
        result.put("severity",severity);
        result.put("windowMinutes",windowMinutes);
        result.put("transactions",total);
        result.put("currentFailureRate",currentFailureRate);
        result.put("baselineFailureRate",baselineFailureRate);
        result.put("failureRateThreshold",failureRateThreshold);
        result.put("currentP95Latency",currentP95);
        result.put("latencyThreshold",latencyThreshold);
        result.put("reasons", reasons);
        return result;
    }

    private double calculatePercentile(List<Integer> values, int percentile) {
        if (values.isEmpty())
            return 0;
        if (values.size() == 1)
            return values.get(0);

        double index = percentile / 100.0 * (values.size() - 1);

        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper)
            return values.get(lower);

        double weight = index - lower;

        return values.get(lower) + weight * (values.get(upper) - values.get(lower));
    }
}
