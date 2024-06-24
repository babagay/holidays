package com.proxiad.holidaysapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class SimpleTestConfig {

    /**
     *  https://www.baeldung.com/spring-bean-scopes
     *  https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-factory-scopes
     */
    @Bean
    @Scope(scopeName = "prototype")
    public TestBeanLifeCycle getTestBeanLifeCycle(){
        TestBeanLifeCycle bean = new TestBeanLifeCycle();
        bean.setValue(2);
        return bean;
    }

    /**
     *  https://www.baeldung.com/spring-bean-scopes
     *  https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-factory-scopes
     */
    @Bean
    @Scope(scopeName = "singleton")
    public SomeAnotherBean getSomeAnotherBean(){
        SomeAnotherBean bean = new SomeAnotherBean();
        bean.setValue(12);
        return bean;
    }
}
