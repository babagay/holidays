package com.proxiad.holidaysapp.test_service;

import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
public class TestTransactionalService {

    private final String beanName;
    private final TransactionTemplate transactionTemplate;

    public TestTransactionalService(@Value("${test.bean.name}") String beanName,
                                    TransactionTemplate transactionTemplate) {
        this.beanName = beanName;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Private method making Transactional problem
     * By default internal method call will be executed in the initial transaction.
     * If we need a nested transaction, there are 3 ways:
     * - explicitly create a new transaction
     * - move code into separate class
     * - make a self-injection
     * - use EGC compiler instead CGLIB
     */
    public void invokeInTransaction() {

        int a = 1, b = 2;
        internalTransactionalMethod(); // nested transaction wont be created coz its a the same class method call

        System.out.println(a + b);

        transactionTemplate.execute(status -> { // explicitly create a new transaction
            internalNonTransactionalMethod();
            return 1;
        });
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void internalTransactionalMethod() {
        int c = 10, d = 30;
        System.out.println(c + d);
    }

    private void internalNonTransactionalMethod() {
        int c = 5, d = 3;
        System.out.println(c + d);
    }

    @Transactional(timeout = 1)
    @SneakyThrows
    public void longTransaction(){
        int c = 4, d = 6;
        Thread.sleep(10_200); // does not work


        System.out.println(c + d);
    }
}
