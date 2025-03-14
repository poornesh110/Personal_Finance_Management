package com.pfm.repository;

import com.pfm.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountUserIdAndDateAfterAndTransactionType(Long userId, LocalDateTime startDate, String expense);

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountUserIdAndCategoryIdAndDateBetween(Long userId, Long id, LocalDateTime startDate, LocalDateTime endDate);

    List<Object[]> findCategorySpendingByUserIdAndDateAfter(Long userId, LocalDateTime startDate);
}
