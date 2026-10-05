package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReliabilityBaselineService {
    private final TransactionRepository repository;

    public ReliabilityBaselineService(TransactionRepository repository){
        this.repository=repository;
    }

    public Map<String, Object> getBaseline(int windowMinutes, int numberOfWindows){

        if(windowMinutes<=0)
            throw new IllegalArgumentException("Window minutes must be greater than 0");

        if(numberOfWindows<=0)
            throw new IllegalArgumentException("Number of windows must be greater than 0");

        LocalDateTime now=LocalDateTime.now();

        List<Transaction> transactions = repository.findAll().stream()
                .filter(t-> t.getTimestamp()!=null)
                .toList();

        List<Double> failureRates=new ArrayList<>();
        List<Double> averageLatencies=new ArrayList<>();
        List<Double> p95Latencies=new ArrayList<>();
        List<Double> tpsValues=new ArrayList<>();

        /*
         * We deliberately look at previous windows.
         *
         * current window is excluded because it will be compared against the baseline later.
         */

        for(int i=1;i<=numberOfWindows;i++){

            LocalDateTime windowEnd= now.minusMinutes( (long) windowMinutes * i);

            LocalDateTime windowStart= windowEnd.minusMinutes(windowMinutes);

            List<Transaction> windowTransactions = transactions.stream()
                    .filter(t-> !t.getTimestamp().isBefore(windowStart) && t.getTimestamp().isBefore(windowEnd))
                    .toList();

            if(windowTransactions.isEmpty()){
                continue;
            }

            long total= windowTransactions.size();

            long failures = windowTransactions.stream()
                    .filter(t -> t.getStatus().equalsIgnoreCase("FAILED"))
                    .count();

            double failureRate = (total == 0) ? 0: failures*100.0/total;

            double tps = total/(windowMinutes*60.0);

            double averageLatency = windowTransactions.stream()
                    .filter(t-> t.getLatencyMs() != null)
                    .mapToInt(Transaction::getLatencyMs)
                    .average()
                    .orElse(0);

            List<Integer> latencies = windowTransactions.stream()
                    .map(Transaction::getLatencyMs)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();

            double p95= calculatePercentile(latencies, 95);

            failureRates.add(failureRate);
            averageLatencies.add(averageLatency);
            p95Latencies.add(p95);
            tpsValues.add(tps);
        }
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("windowMinutes",windowMinutes);
        result.put("requestedWindows",numberOfWindows);
        result.put("availableWindows",failureRates.size());
        result.put("baselineFailureRate", average(failureRates));
        result.put("baselineP95Latency", average(p95Latencies));
        result.put("baselineTps", average(tpsValues));
        result.put("failureRateStdDev", standardDeviation(failureRates));
        result.put("latencyStdDev", standardDeviation(averageLatencies));

        return result;
    }

    private double average(List<Double> values){
        if(values.isEmpty())
            return 0;

        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }

    private double standardDeviation(List<Double> values){
        if(values.size()<=1)
            return 0;

        double mean = average(values);

        double variance = values.stream()
                .mapToDouble(value -> Math.pow((value-mean),2))
                .average()
                .orElse(0);

        return Math.sqrt(variance);
    }

    private double calculatePercentile(List<Integer> values, double percentile){
        if(values.isEmpty())
            return 0;

        if(values.size()==1)
            return values.get(0);

        double index= percentile/100.0 * (values.size()-1);

        int lower= (int) Math.floor(index);

        int upper= (int) Math.ceil(index);

        if(lower==upper)
            return values.get(lower);

        double weight = index - lower;

        return values.get(lower) + weight * ( values.get(upper) - values.get(lower));
    }
}
