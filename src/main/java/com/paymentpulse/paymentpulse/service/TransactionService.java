package com.paymentpulse.paymentpulse.service;

import com.paymentpulse.paymentpulse.model.Transaction;
import com.paymentpulse.paymentpulse.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository){
        this.repository=repository;
    }

    public Transaction createTransaction(Transaction transaction){
        return repository.save(transaction);
    }

    public List<Transaction> getAllTransactions(){
        return repository.findAll();
    }

    public Transaction getTransaction(Long id){
        return repository.findById(id).orElse(null);
    }
}
