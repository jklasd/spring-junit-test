 package com.github.jklasd.test.spring;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyListableBeanFactory extends DefaultListableBeanFactory{

    public LazyListableBeanFactory(BeanFactory arg0) {
        super(arg0);
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
        throws BeanDefinitionStoreException {
//         super.registerBeanDefinition(beanName, beanDefinition);
//        log.info("registerBeanDefinition===={}",beanName);
    }
}
