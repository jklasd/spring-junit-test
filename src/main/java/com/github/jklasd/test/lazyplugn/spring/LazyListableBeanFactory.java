 package com.github.jklasd.test.lazyplugn.spring;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.model.BeanModel;
import com.github.jklasd.test.util.ScanUtil;

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
        log.info("registerBeanDefinition===={}",beanName);
        if(!TestUtil.getInstance().getApplicationContext().containsBean(beanName)) {
            Class<?> tagC = ScanUtil.loadClass(beanDefinition.getBeanClassName());
            BeanModel beanModel = new BeanModel();
            beanModel.setBeanName(beanName);
            beanModel.setPropValue(beanDefinition.getPropertyValues());
            beanModel.setConstructorArgValue(beanDefinition.getConstructorArgumentValues());
            beanModel.setTagClass(tagC);
            beanModel.setXmlBean(true);
            Object newBean = LazyBean.getInstance().buildProxy(beanModel);
            TestUtil.getInstance().getApplicationContext().registBean(beanName, newBean, tagC);
        }
    }
}
