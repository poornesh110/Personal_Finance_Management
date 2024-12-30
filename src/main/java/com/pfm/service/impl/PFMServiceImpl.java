package com.pfm.service.impl;

import com.pfm.entity.Transaction;
import com.pfm.repository.TransactionRepository;
import com.pfm.service.PFMService;
import com.pfm.util.AutowiredHelper;

public class PFMServiceImpl implements PFMService {

    private final AutowiredHelper autowiredHelper;

    public PFMServiceImpl(AutowiredHelper autowiredHelper) {
        this.autowiredHelper = autowiredHelper;
    }

    @Override
    public Object getAllTransactions() {
        TransactionRepository transactionRepository = autowiredHelper.getTransactionRepository();
        return transactionRepository.findAll();
    }

    @Override
    public Object addTransaction(Transaction transaction) {
        TransactionRepository transactionRepository = autowiredHelper.getTransactionRepository();
        return transactionRepository.save(transaction);
    }

    @Override
    public Object deleteTransaction(Long id) {
        TransactionRepository transactionRepository = autowiredHelper.getTransactionRepository();
        transactionRepository.deleteById(id);
        return "Deleted Successfully";
    }
}
