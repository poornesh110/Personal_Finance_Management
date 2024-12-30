package com.pfm.service;

import com.pfm.entity.Transaction;
import org.springframework.stereotype.Service;

@Service
public interface PFMService {
    Object getAllTransactions();

    Object addTransaction(Transaction transaction);

    Object deleteTransaction(Long id);
}
