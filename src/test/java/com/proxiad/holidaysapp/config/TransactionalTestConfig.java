package com.proxiad.holidaysapp.config;

import com.proxiad.holidaysapp.test_service.TestTransactionalService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.mock;

/**
 * Read more
 * https://www.baeldung.com/spring-bean
 * https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-definition
 */
//@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {"com.proxiad.holidaysapp.test_service"})
@Profile("test")
public class TransactionalTestConfig {

    @Bean
    @Scope(scopeName = "prototype")
    public TestBeanLifeCycle getTestBeanLifeCycle() {
        TestBeanLifeCycle bean = new TestBeanLifeCycle();
        bean.setValue(10);
        return bean;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return mock(PlatformTransactionManager.class);
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager){
        return new TransactionTemplate(transactionManager);
    }

    /**
     * https://javarevisited.blogspot.com/2021/08/spring-transactional-example-how-to.html
     */
//    @Bean
//    public TestTransactionalService testTransactionalService(TransactionTemplate transactionTemplate){
//        return new TestTransactionalService("beanName", transactionTemplate);
//    }
}
