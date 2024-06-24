package com.proxiad.holidaysapp;

import com.proxiad.holidaysapp.config.TransactionalTestConfig;
import com.proxiad.holidaysapp.test_service.TestTransactionalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ContextConfiguration(classes = {TransactionalTestConfig.class})
@SpringBootTest
class TransactionalTest {

    @Autowired
    private BeanFactory beanFactory;

    @Autowired
    private TestTransactionalService service;

    @Test
    @DisplayName("Get service wrapped in CGLIB proxy")
    void shouldGetProxy() {
        // Get service via factory
        // TestTransactionalService bean = beanFactory.getBean(TestTransactionalService.class);

        assertNotNull(service);
        service.invokeInTransaction();
    }

    @Test
    @DisplayName("Get service ")
    void testFoo(){
        service.longTransaction();
    }
}
