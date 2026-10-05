package com.paymentpulse.paymentpulse.repository;

import com.paymentpulse.paymentpulse.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction,Long>{

    }
