package com.paymentpulse.paymentpulse.controller;

import com.paymentpulse.paymentpulse.event.TransactionCreatedEvent;
import com.paymentpulse.paymentpulse.service.TransactionProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/kafka")
public class KafkaTestController {

    private final TransactionProducerService producerService;

    public KafkaTestController(TransactionProducerService producerService){
        this.producerService = producerService;
    }

    @PostMapping("/test")
    public CompletableFuture<ResponseEntity<String>> publishTestEvent(@RequestParam(required = false) String eventId) {
        TransactionCreatedEvent event = new TransactionCreatedEvent();

        event.setEventId(eventId==null || eventId.isBlank() ? UUID.randomUUID().toString(): eventId);
        event.setMerchantId("M_TEST");
        event.setBankId("B_TEST");
        event.setGatewayId("G_TEST");
        event.setAmount(1500.0);
        event.setLocation("Kanpur");
        event.setPaymentMethod("UPI");
        event.setStatus("SUCCESS");
        event.setResponseCode("00");
        event.setLatencyMs(250);
        event.setTimestamp(LocalDateTime.now());

        return producerService.publish(event).thenApply(result -> ResponseEntity.ok("Test event successfully published. Event ID: "+ event.getEventId()));
    }
}
