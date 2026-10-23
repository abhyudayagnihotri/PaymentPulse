package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FailureCorrelationService {
    private static final int MIN_PAIR_TRANSACTIONS =10;
    private static final double FAILURE_RATE_GAP = 10.0;
    private final TransactionRepository repository;

    public FailureCorrelationService(TransactionRepository repository){
        this.repository = repository;
    }

    public Map<String, Object> analyze(int windowMinutes){

        if(windowMinutes<=0){
            throw new IllegalArgumentException("windowMinutes should be greater than 0");
        }

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMinutes(windowMinutes);

        List<Transaction> transactions = repository.findAll().stream()
                .filter(t->t.getTimestamp()!=null && t.getTimestamp().isBefore(end) && !t.getTimestamp().isBefore(start))
                .toList();

        long totalFailures = transactions.stream()
                .filter(this::isFailed)
                .count();

        double overallFailureRate = transactions.isEmpty() ? 0.0 : totalFailures * 100.0 / transactions.size();

        // Group transactions by gateway-bank combination
        Map<PairKey, List<Transaction>> grouped = transactions.stream().collect(Collectors.groupingBy(
                t -> new PairKey(normalize(t.getGatewayId()),normalize(t.getBankId()))
        ));

        List<Map<String, Object>> pairs = new ArrayList<>();

        for(Map.Entry<PairKey, List<Transaction>> entry : grouped.entrySet()) {
            PairKey key = entry.getKey();
            List<Transaction> group = entry.getValue();

            List<Transaction> failures = group.stream()
                    .filter(this::isFailed)
                    .toList();

            double failureRate = failures.size() * 100.0 / group.size();

            double averageLatency = group.stream()
                    .filter(t -> t.getLatencyMs() != null)
                    .mapToInt(Transaction::getLatencyMs)
                    .average()
                    .orElse(0.0);

            Map<String, Long> responseCodes = failures.stream()
                    .collect(Collectors.groupingBy(
                            t-> normalize(t.getResponseCode()),
                            TreeMap::new,
                            Collectors.counting()
                    ));

            boolean sufficientData = group.size() >= MIN_PAIR_TRANSACTIONS;

            boolean suspicious = sufficientData && failureRate >= overallFailureRate + FAILURE_RATE_GAP;

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("gatewayId",key.gatewayId());
            result.put("bankId",key.bankId());
            result.put("transactions",group.size());
            result.put("failures", failures.size());
            result.put("failureRate",failureRate);
            result.put("averageLatency", averageLatency);
            result.put("responseCodes", responseCodes);
            result.put("sufficientData", sufficientData);
            result.put("suspiciousSignal", suspicious);

            if(suspicious){
                result.put("signalReason","Failure rate is at least "+FAILURE_RATE_GAP + " percentage points above " + "the overall window rate");
            }
            else if (!sufficientData){
                result.put("signalReason","Insufficient transactions for a reliable signal");
            }
            else{
                result.put("signalReason", "No elevated failure-rate signal detected");
            }
            pairs.add(result);
        }

        // Suspicious Combinations first, then highest failure rate.
        pairs.sort((a,b)->{
            boolean aSuspicious = Boolean.TRUE.equals(a.get("suspiciousSignal"));
            boolean bSuspicious = Boolean.TRUE.equals(b.get("suspiciousSignal"));

            if(aSuspicious != bSuspicious){
                return Boolean.compare(bSuspicious, aSuspicious);
            }

            return Double.compare(((Number) b.get("failureRate")).doubleValue(),((Number) a.get("failureRate")).doubleValue());
        });

        Map<String, Long> globalResponseCodes = transactions.stream()
                .filter(this::isFailed)
                .collect(Collectors.groupingBy(
                        t->normalize(t.getResponseCode()),
                        TreeMap::new,
                        Collectors.counting()
                ));

        Map <String, Object> response = new LinkedHashMap<>();

        response.put("windowMinutes", windowMinutes);
        response.put("windowStart", start);
        response.put("windowEnd", end);
        response.put("currentTransactions", transactions.size());
        response.put("failedTransactions",totalFailures);
        response.put("overallFailureRate", overallFailureRate);
        response.put("failureCodes", globalResponseCodes);
        response.put("gatewayBankAnalysis", pairs);

        return response;
    }

    private boolean isFailed(Transaction transaction) {
        return "FAILED".equalsIgnoreCase(transaction.getStatus());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value;
    }

    private record PairKey(String gatewayId, String bankId){
    }
}
