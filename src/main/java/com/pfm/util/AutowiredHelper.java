package com.pfm.util;

import com.pfm.repository.TransactionRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AutowiredHelper {

    @Autowired
    private TransactionRepository transactionRepository;
}
