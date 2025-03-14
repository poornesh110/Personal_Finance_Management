package com.pfm.service;

import com.pfm.entity.Budget;
import com.pfm.entity.Transaction;
import com.pfm.repository.BudgetRepository;
import com.pfm.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BudgetAnalysisService {
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public BudgetAnalysisService(
            BudgetRepository budgetRepository,
            TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
    }

    // Create a new budget
    public Budget createBudget(Budget budget) {
        return budgetRepository.save(budget);
    }

    // DSA Implementation: Graph-based budget analysis
    public Map<String, Object> analyzeBudgetPerformance(Long userId) {
        // Get all budgets for a user
        List<Budget> budgets = budgetRepository.findByUserId(userId);

        // Create an adjacency list representation of budget categories
        Map<Long, List<BudgetNode>> budgetGraph = new HashMap<>();

        for (Budget budget : budgets) {
            LocalDateTime startDate = budget.getStartDate();
            LocalDateTime endDate = budget.getEndDate();

            // Get all transactions for this category within budget date range
            List<Transaction> transactions = transactionRepository.findByAccountUserIdAndCategoryIdAndDateBetween(
                    userId, budget.getCategory().getId(), startDate, endDate);

            // Calculate actual spending
            BigDecimal actualSpending = transactions.stream()
                    .filter(t -> "EXPENSE".equals(t.getTransactionType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Create budget node
            BudgetNode node = new BudgetNode(
                    budget.getCategory().getId(),
                    budget.getCategory().getName(),
                    budget.getAmount(),
                    actualSpending
            );

            // Add to graph
            if (!budgetGraph.containsKey(budget.getCategory().getId())) {
                budgetGraph.put(budget.getCategory().getId(), new ArrayList<>());
            }
            budgetGraph.get(budget.getCategory().getId()).add(node);
        }

        // Analysis results
        Map<String, Object> result = new HashMap<>();

        // Calculate budget adherence percentage
        Map<String, BigDecimal> adherenceByCategory = new HashMap<>();
        Map<String, BigDecimal> totalBudgetByCategory = new HashMap<>();
        Map<String, BigDecimal> totalSpendingByCategory = new HashMap<>();

        for (Map.Entry<Long, List<BudgetNode>> entry : budgetGraph.entrySet()) {
            String categoryName = entry.getValue().get(0).categoryName;

            BigDecimal totalBudget = entry.getValue().stream()
                    .map(node -> node.budgetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalSpending = entry.getValue().stream()
                    .map(node -> node.actualSpending)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal adherencePercentage;
            if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
                adherencePercentage = totalSpending.multiply(new BigDecimal("100"))
                        .divide(totalBudget, 2, RoundingMode.HALF_UP);
            } else {
                adherencePercentage = new BigDecimal("100");
            }

            adherenceByCategory.put(categoryName, adherencePercentage);
            totalBudgetByCategory.put(categoryName, totalBudget);
            totalSpendingByCategory.put(categoryName, totalSpending);
        }

        result.put("adherenceByCategory", adherenceByCategory);
        result.put("totalBudgetByCategory", totalBudgetByCategory);
        result.put("totalSpendingByCategory", totalSpendingByCategory);

        // Find categories that are overspent
        List<Map<String, Object>> overspentCategories = new ArrayList<>();
        for (String category : adherenceByCategory.keySet()) {
            BigDecimal adherence = adherenceByCategory.get(category);
            if (adherence.compareTo(new BigDecimal("100")) > 0) {
                Map<String, Object> overspent = new HashMap<>();
                overspent.put("category", category);
                overspent.put("budgetAmount", totalBudgetByCategory.get(category));
                overspent.put("actualSpending", totalSpendingByCategory.get(category));
                overspent.put("overBudgetAmount", totalSpendingByCategory.get(category).subtract(totalBudgetByCategory.get(category)));
                overspent.put("percentageOver", adherence.subtract(new BigDecimal("100")));
                overspentCategories.add(overspent);
            }
        }
        result.put("overspentCategories", overspentCategories);

        // Find categories that are underspent (potential savings)
        List<Map<String, Object>> underspentCategories = new ArrayList<>();
        for (String category : adherenceByCategory.keySet()) {
            BigDecimal adherence = adherenceByCategory.get(category);
            if (adherence.compareTo(new BigDecimal("100")) < 0) {
                Map<String, Object> underspent = new HashMap<>();
                underspent.put("category", category);
                underspent.put("budgetAmount", totalBudgetByCategory.get(category));
                underspent.put("actualSpending", totalSpendingByCategory.get(category));
                underspent.put("savedAmount", totalBudgetByCategory.get(category).subtract(totalSpendingByCategory.get(category)));
                underspent.put("percentageSaved", new BigDecimal("100").subtract(adherence));
                underspentCategories.add(underspent);
            }
        }
        result.put("underspentCategories", underspentCategories);

        // Calculate overall budget adherence
        BigDecimal totalBudget = totalBudgetByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpending = totalSpendingByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal overallAdherence;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            overallAdherence = totalSpending.multiply(new BigDecimal("100"))
                    .divide(totalBudget, 2, RoundingMode.HALF_UP);
        } else {
            overallAdherence = new BigDecimal("100");
        }

        result.put("overallAdherence", overallAdherence);
        result.put("totalBudget", totalBudget);
        result.put("totalSpending", totalSpending);

        return result;
    }

    // DSA: Use dynamic programming to suggest optimal budget allocation
    public Map<String, BigDecimal> suggestOptimalBudgetAllocation(Long userId, BigDecimal totalBudgetAmount) {
        // Get spending history by category for the last 3 months
        LocalDateTime startDate = LocalDateTime.now().minusMonths(3);
        List<Object[]> categorySpending = transactionRepository.findCategorySpendingByUserIdAndDateAfter(
                userId, startDate);

        // Convert to a more usable format
        List<CategorySpending> categories = new ArrayList<>();
        for (Object[] row : categorySpending) {
            String categoryName = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            categories.add(new CategorySpending(categoryName, amount));
        }

        // Sort by spending amount (descending)
        categories.sort((a, b) -> b.amount.compareTo(a.amount));

        // Calculate total spending
        BigDecimal totalSpending = categories.stream()
                .map(c -> c.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate budget allocation based on spending patterns
        Map<String, BigDecimal> allocation = new HashMap<>();
        for (CategorySpending category : categories) {
            BigDecimal ratio = category.amount.divide(totalSpending, 4, RoundingMode.HALF_UP);
            BigDecimal allocatedAmount = totalBudgetAmount.multiply(ratio)
                    .setScale(2, RoundingMode.HALF_UP);
            allocation.put(category.name, allocatedAmount);
        }

        return allocation;
    }

    // Helper classes
    private static class BudgetNode {
        Long categoryId;
        String categoryName;
        BigDecimal budgetAmount;
        BigDecimal actualSpending;

        public BudgetNode(Long categoryId, String categoryName, BigDecimal budgetAmount, BigDecimal actualSpending) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.budgetAmount = budgetAmount;
            this.actualSpending = actualSpending;
        }
    }

    private static class CategorySpending {
        String name;
        BigDecimal amount;

        public CategorySpending(String name, BigDecimal amount) {
            this.name = name;
            this.amount = amount;
        }
    }
}
