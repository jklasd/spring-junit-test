 package com.github.jklasd.test.lazyplugn.spring;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyListableBeanFactory extends DefaultListableBeanFactory{

    public LazyListableBeanFactory() {
        super(null);
    }
//    public LazyListableBeanFactory(BeanFactory arg0) {
//        super(arg0);
//    }
    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
        throws BeanDefinitionStoreException {
//         super.registerBeanDefinition(beanName, beanDefinition);
//        log.info("registerBeanDefinition===={}",beanName);
//        if(!TestUtil.getApplicationContext().containsBean(beanName)) {
//            Class<?> tagC = ScanUtil.loadClass(beanDefinition.getBeanClassName());
//            BeanModel beanModel = new BeanModel();
//            beanModel.setBeanName(beanName);
//            beanModel.setPropValue(beanDefinition.getPropertyValues());
//            beanModel.setTagClass(tagC);
//            beanModel.setXmlBean(true);
//            Object newBean = LazyBean.buildProxy(beanModel);
//            TestUtil.getApplicationContext().registBean(beanName, newBean, tagC);
//        }
    }
}
