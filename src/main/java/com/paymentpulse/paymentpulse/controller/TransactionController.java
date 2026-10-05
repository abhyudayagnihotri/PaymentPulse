package com.paymentpulse.paymentpulse.controller;

import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    public Transaction createTransaction(
            @RequestBody Transaction transaction) {

        return service.createTransaction(transaction);
    }

    @GetMapping
    public List<Transaction> getTransactions() {
        return service.getAllTransactions();
    }

    @GetMapping("/{id}")
    public Transaction getTransaction(
            @PathVariable Long id) {

        return service.getTransaction(id);
    }
}