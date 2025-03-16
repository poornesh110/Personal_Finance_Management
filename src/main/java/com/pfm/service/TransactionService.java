package com.pfm.service;

import com.pfm.entity.Account;
import com.pfm.entity.Category;
import com.pfm.entity.Transaction;
import com.pfm.repository.AccountRepository;
import com.pfm.repository.CategoryRepository;
import com.pfm.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    @Autowired
    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Transaction addTransaction(Transaction transaction, Long accountId, Long categoryId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setDate(LocalDateTime.now());

        // Update account balance
        BigDecimal newBalance;
        if ("INCOME".equals(transaction.getTransactionType())) {
            newBalance = account.getBalance().add(transaction.getAmount());
        } else {
            newBalance = account.getBalance().subtract(transaction.getAmount());
        }
        account.setBalance(newBalance);
        accountRepository.save(account);

        return transactionRepository.save(transaction);
    }

    // DSA Implementation: Get spending trends using Sliding Window algorithm
    public Map<String, BigDecimal> getSpendingTrendsByCategory(Long userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Transaction> transactions = transactionRepository.findByAccountUserIdAndDateAfterAndTransactionType(
                userId, startDate, "EXPENSE");

        // Group by category and sum
        Map<String, BigDecimal> categoryTotals = new HashMap<>();

        for (Transaction transaction : transactions) {
            String categoryName = transaction.getCategory().getName();
            BigDecimal currentAmount = categoryTotals.getOrDefault(categoryName, BigDecimal.ZERO);
            categoryTotals.put(categoryName, currentAmount.add(transaction.getAmount()));
        }

        return categoryTotals;
    }

    // DSA Implementation: Binary Search for finding transactions within amount range
    public List<Transaction> findTransactionsInRange(Long accountId, BigDecimal minAmount, BigDecimal maxAmount) {
        List<Transaction> allTransactions = transactionRepository.findByAccountId(accountId);

        // Sort transactions by amount
        allTransactions.sort(Comparator.comparing(Transaction::getAmount));

        // Binary search for lower bound
        int lowerIndex = binarySearchLowerBound(allTransactions, minAmount);

        // Binary search for upper bound
        int upperIndex = binarySearchUpperBound(allTransactions, maxAmount);

        if (lowerIndex <= upperIndex) {
            return allTransactions.subList(lowerIndex, upperIndex + 1);
        }

        return Collections.emptyList();
    }

    private int binarySearchLowerBound(List<Transaction> transactions, BigDecimal target) {
        int left = 0;
        int right = transactions.size() - 1;
        int result = transactions.size(); // Default if no element is found

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (transactions.get(mid).getAmount().compareTo(target) >= 0) {
                result = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return result;
    }

    private int binarySearchUpperBound(List<Transaction> transactions, BigDecimal target) {
        int left = 0;
        int right = transactions.size() - 1;
        int result = -1; // Default if no element is found

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (transactions.get(mid).getAmount().compareTo(target) <= 0) {
                result = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return result;
    }

    // DSA Implementation: Use priority queue for getting top spending categories
    public List<Map.Entry<String, BigDecimal>> getTopSpendingCategories(Long userId, int limit) {
        Map<String, BigDecimal> categoryTotals = getSpendingTrendsByCategory(userId, 30); // Last 30 days

        // Use a min-heap priority queue for getting top elements
        PriorityQueue<Map.Entry<String, BigDecimal>> pq = new PriorityQueue<>(
                Comparator.comparing(Map.Entry::getValue));

        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            pq.offer(entry);
            if (pq.size() > limit) {
                pq.poll(); // Remove smallest element
            }
        }

        // Convert to list and reverse (to get descending order)
        List<Map.Entry<String, BigDecimal>> result = new ArrayList<>();
        while (!pq.isEmpty()) {
            result.add(pq.poll());
        }
        Collections.reverse(result);

        return result;
    }

    // DSA Implementation: Using a custom trie for auto-categorization based on description
    private final CategorizationTrie categorizationTrie = new CategorizationTrie();

    public void trainCategorizationModel(List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            String description = transaction.getDescription().toLowerCase();
            String category = transaction.getCategory().getName();
            categorizationTrie.insert(description, category);
        }
    }

    public String suggestCategory(String description) {
        return categorizationTrie.findCategory(description.toLowerCase());
    }

    // Custom Trie implementation for categorization
    private static class CategorizationTrie {
        private final TrieNode root;

        public CategorizationTrie() {
            this.root = new TrieNode();
        }

        public void insert(String description, String category) {
            String[] words = description.split("\\s+");
            for (String word : words) {
                insertWord(word, category);
            }
        }

        private void insertWord(String word, String category) {
            TrieNode current = root;
            for (char c : word.toCharArray()) {
                current.children.putIfAbsent(c, new TrieNode());
                current = current.children.get(c);
            }
            current.isEndOfWord = true;
            current.categories.put(category, current.categories.getOrDefault(category, 0) + 1);
        }

        public String findCategory(String description) {
            Map<String, Integer> categoryCounts = new HashMap<>();
            String[] words = description.split("\\s+");

            for (String word : words) {
                String category = findCategoryForWord(word);
                if (category != null) {
                    categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
                }
            }

            // Find category with highest count
            return categoryCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Uncategorized");
        }

        private String findCategoryForWord(String word) {
            TrieNode current = root;
            for (char c : word.toCharArray()) {
                if (!current.children.containsKey(c)) {
                    return null;
                }
                current = current.children.get(c);
            }

            if (!current.isEndOfWord) {
                return null;
            }

            // Return the most frequent category for this word
            return current.categories.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }

        private static class TrieNode {
            Map<Character, TrieNode> children = new HashMap<>();
            boolean isEndOfWord;
            Map<String, Integer> categories = new HashMap<>();
        }

    }
}
