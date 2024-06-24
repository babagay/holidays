package com.proxiad.holidaysapp;

import com.proxiad.holidaysapp.config.SimpleTestConfig;
import com.proxiad.holidaysapp.config.SomeAnotherBean;
import com.proxiad.holidaysapp.config.TestBeanLifeCycle;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration(classes = {SimpleTestConfig.class})
@SpringBootTest
class HolidaysAppApplicationTests {

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	private TestBeanLifeCycle testBeanLifeCycle;

	@Autowired
	private SomeAnotherBean someAnotherBean;

	@Test
	@DisplayName("Should check two prototype beans are different objects")
	void test_twoInjectedPrototypeBeans_Different() {
		TestBeanLifeCycle bean = beanFactory.getBean(TestBeanLifeCycle.class);

		assertNotNull(bean, "empty"); // bean 1
		assertNotNull(testBeanLifeCycle, "empty too"); // bean 2
		assertEquals(bean, testBeanLifeCycle); // beans has similar state, they are equivalent
		assertNotSame(bean, testBeanLifeCycle); // beans are Not the same object
	}

	@Test
	@DisplayName("Should check two singleton beans refer to the same object")
	void test_twoInjectedSingletonBeans_Same() {
		SomeAnotherBean bean = beanFactory.getBean(SomeAnotherBean.class);
		assertEquals(bean, someAnotherBean); // beans are equals (their state is equal)
		assertSame(bean, someAnotherBean); // beans are the same. They refer to the same memory address
	}

}
