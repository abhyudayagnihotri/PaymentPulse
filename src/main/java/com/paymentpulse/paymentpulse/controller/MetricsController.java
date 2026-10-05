package com.paymentpulse.paymentpulse.controller;

import com.paymentpulse.paymentpulse.service.AnomalyDetectionService;
import com.paymentpulse.paymentpulse.service.MetricsService;
import com.paymentpulse.paymentpulse.service.ReliabilityBaselineService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class MetricsController {

    private final MetricsService metricsService;
    private final ReliabilityBaselineService baselineService;
    private final AnomalyDetectionService anomalyDetectionService;

    public MetricsController(
            MetricsService metricsService,
            ReliabilityBaselineService baselineService,
            AnomalyDetectionService anomalyDetectionService){
        this.metricsService=metricsService;
        this.baselineService = baselineService;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @GetMapping("/api/metrics")
    public Map<String, Object> getMetrics(){

        Map<String, Object> metrics =new LinkedHashMap<>();

        metrics.put("totalTransactions", metricsService.getTotalTransactions());

        metrics.put("successfulTransactions", metricsService.getSuccessfulTransactions());

        metrics.put("failedTransactions", metricsService.getFailedTransactions());

        metrics.put("successRate", metricsService.getSuccessRate());

        metrics.put("failureRate", metricsService.getFailureRate());

        metrics.put("averageLatency", metricsService.getAverageLatency());

        metrics.put("gatewayBreakdown", metricsService.getGatewayBreakdown());

        metrics.put("bankBreakdown", metricsService.getBankBreakdown());

        metrics.put("merchantBreakdown", metricsService.getMerchantBreakdown());

        metrics.put("paymentMethodBreakdown", metricsService.getPaymentMethodBreakdown());

        metrics.put("locationBreakdown", metricsService.getLocationBreakdown());

        metrics.put("responseCodeBreakdown",metricsService.getResponseCodeBreakdown());

        return metrics;
    }

    @GetMapping("/api/metrics/window")
    public Map<String, Object> getWindowMetrics( @RequestParam(defaultValue="10") int minutes){
        return metricsService.getTimeWindowMetrics(minutes);
    }

    @GetMapping("/api/metrics/baseline")
    public Map<String, Object> getBaseline(@RequestParam(defaultValue = "10") int windowMinutes, @RequestParam(defaultValue = "6") int windows) {
        return baselineService.getBaseline(windowMinutes, windows);
    }

    @GetMapping("/api/metrics/anomaly")
    public Map<String, Object> detectAnomaly(@RequestParam(defaultValue = "10") int windowMinutes, @RequestParam(defaultValue = "6") int windows){
        return anomalyDetectionService.detectAnomaly(windowMinutes, windows);
    }
}
