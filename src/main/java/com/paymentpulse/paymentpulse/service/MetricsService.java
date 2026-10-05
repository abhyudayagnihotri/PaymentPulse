package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.time.LocalDateTime;

@Service
public class MetricsService {

    private final TransactionRepository repository;

    public MetricsService(TransactionRepository repository) {
        this.repository = repository;
    }

    public long getTotalTransactions() {
        return repository.count();
    }

    public long getSuccessfulTransactions() {

        return repository.findAll()
                .stream()
                .filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus()))
                .count();
    }

    public long getFailedTransactions() {

        return repository.findAll()
                .stream()
                .filter(t -> "FAILED".equalsIgnoreCase(t.getStatus()))
                .count();
    }

    public double getSuccessRate() {

        long total = getTotalTransactions();

        if (total == 0) {
            return 0;
        }

        return (getSuccessfulTransactions() * 100.0) / total;
    }

    public double getFailureRate() {

        long total = getTotalTransactions();

        if (total == 0) {
            return 0;
        }

        return (getFailedTransactions() * 100.0) / total;
    }

    public double getAverageLatency() {

        List<Transaction> transactions = repository.findAll();

        if (transactions.isEmpty()) {
            return 0;
        }

        return transactions.stream()
                .filter(t -> t.getLatencyMs() != null)
                .mapToInt(Transaction::getLatencyMs)
                .average()
                .orElse(0);
    }


    // ---------------------------------------------------------
    // GENERIC BREAKDOWN
    // ---------------------------------------------------------

    private Map<String, Map<String, Object>> getBreakdown(
            Function<Transaction, String> keyExtractor) {

        List<Transaction> transactions = repository.findAll();

        Map<String, Map<String, Object>> result =
                new LinkedHashMap<>();

        for (Transaction transaction : transactions) {

            String key = keyExtractor.apply(transaction);

            if (key == null || key.isBlank()) {
                key = "UNKNOWN";
            }

            Map<String, Object> metrics = result.computeIfAbsent(
                    key,
                    k -> {

                        Map<String, Object> map =
                                new LinkedHashMap<>();

                        map.put("transactions", 0L);
                        map.put("successes", 0L);
                        map.put("failures", 0L);
                        map.put("failureRate", 0.0);
                        map.put("averageLatency", 0.0);

                        return map;
                    }
            );

            // -------------------------
            // Transaction count
            // -------------------------

            long total =
                    (Long) metrics.get("transactions");

            metrics.put(
                    "transactions",
                    total + 1
            );


            // -------------------------
            // Success / Failure
            // -------------------------

            if ("SUCCESS".equalsIgnoreCase(
                    transaction.getStatus())) {

                long successes =
                        (Long) metrics.get("successes");

                metrics.put(
                        "successes",
                        successes + 1
                );

            } else if ("FAILED".equalsIgnoreCase(
                    transaction.getStatus())) {

                long failures =
                        (Long) metrics.get("failures");

                metrics.put(
                        "failures",
                        failures + 1
                );
            }


            // -------------------------
            // Average latency
            // -------------------------

            if (transaction.getLatencyMs() != null) {

                double currentAverage =
                        (Double) metrics.get("averageLatency");

                long transactionCount =
                        (Long) metrics.get("transactions");

                double newAverage =
                        (
                                currentAverage
                                        * (transactionCount - 1)
                                        + transaction.getLatencyMs()
                        )
                                / transactionCount;

                metrics.put(
                        "averageLatency",
                        newAverage
                );
            }
        }


        // -----------------------------------------------------
        // Calculate failure rate
        // -----------------------------------------------------

        for (Map<String, Object> metrics : result.values()) {

            long total =
                    (Long) metrics.get("transactions");

            long failures =
                    (Long) metrics.get("failures");

            double failureRate =
                    total == 0
                            ? 0
                            : (failures * 100.0) / total;

            metrics.put(
                    "failureRate",
                    failureRate
            );
        }

        return result;
    }


    // ---------------------------------------------------------
    // BREAKDOWNS
    // ---------------------------------------------------------

    public Map<String, Map<String, Object>> getGatewayBreakdown() {

        return getBreakdown(
                Transaction::getGatewayId
        );
    }


    public Map<String, Map<String, Object>> getBankBreakdown() {

        return getBreakdown(
                Transaction::getBankId
        );
    }


    public Map<String, Map<String, Object>> getMerchantBreakdown() {

        return getBreakdown(
                Transaction::getMerchantId
        );
    }


    public Map<String, Map<String, Object>> getPaymentMethodBreakdown() {

        return getBreakdown(
                Transaction::getPaymentMethod
        );
    }


    public Map<String, Map<String, Object>> getLocationBreakdown() {

        return getBreakdown(
                Transaction::getLocation
        );
    }


    public Map<String, Object> getTimeWindowMetrics(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Minutes must be greater than 0");
        }

        LocalDateTime end = LocalDateTime.now();

        LocalDateTime start = end.minusMinutes(minutes);

        List<Transaction> transactions = repository.findAll()
                .stream()
                .filter(t -> t.getTimestamp() != null)
                .filter(t -> !t.getTimestamp().isBefore(start) && !t.getTimestamp().isAfter(end))
                .toList();

        long total = transactions.size();

        long successes = transactions.stream()
                .filter(t -> (t.getStatus()).equalsIgnoreCase("SUCCESS"))
                .count();

        long failures = transactions.stream()
                .filter(t -> (t.getStatus()).equalsIgnoreCase("FAILED"))
                .count();

        double failureRate = failures * 100.0 / total;

        double successRate = successes * 100.0 / total;

        double tps = total / (minutes * 60.0);

        List<Integer> latencies = transactions.stream()
                .map(Transaction::getLatencyMs)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        double averageLatency = latencies.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        double p50 = calculatePercentile(latencies, 50);
        double p95 = calculatePercentile(latencies, 95);
        double p99 = calculatePercentile(latencies, 99);

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("windowMinutes", minutes);
        result.put("transactions", total);
        result.put("successfulTransactions", successes);
        result.put("failedTransactions", failures);
        result.put("successRate", successRate);
        result.put("failureRate", failureRate);
        result.put("tps", tps);
        result.put("averageLatency", averageLatency);
        result.put("p50Latency", p50);
        result.put("p95Latency", p95);
        result.put("p99Latency", p99);

        return result;

    }

    private double calculatePercentile(List<Integer> values, double percentile) {
        if (values.isEmpty())
            return 0;

        if (values.size() == 1)
            return values.get(0);

        double index = (percentile/100.0) * (values.size() -1);

        int lower = (int) Math.floor(index);

        int upper = (int ) Math.ceil(index);

        if(lower==upper)
            return values.get(lower);

        double weight = index - lower;

        return values.get(lower) + weight * (values.get(upper) - values.get(lower));
    }

    public Map<String, Long> getResponseCodeBreakdown() {
        List<Transaction> transactions = repository.findAll();

        Map<String, Long> result = new LinkedHashMap<>();

        for(Transaction transaction: transactions){

            String code = transaction.getResponseCode();

            if(code == null || code.isBlank()){
                code="UNKNOWN";
            }

            result.put(code, result.getOrDefault(code,0L)+1);
        }

        return result;
    }
}