package com.github.jklasd.test.lazyplugn.spring;

import javax.annotation.Nullable;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyListableBeanFactory extends DefaultListableBeanFactory {
	protected LazyListableBeanFactory() {}
	private static LazyListableBeanFactory beanFactory = new LazyListableBeanFactory();
	public static LazyListableBeanFactory getInstance() {
		return beanFactory;
	}

//    public LazyListableBeanFactory(BeanFactory arg0) {
//        super(arg0);
//    }
	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {
        super.registerBeanDefinition(beanName, beanDefinition);
	}

//	private void registerAnnBean(String beanName, BeanDefinition beanDefinition) {
//		log.debug("registerAnnBean registerBeanDefinition===={}", beanName);
//		super.registerBeanDefinition(beanName, beanDefinition);
//	}
	
	protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {
		Object obj = super.getObjectForBeanInstance(beanInstance, name, beanName, mbd);
		LazyBean.getInstance().processAttr(obj, obj.getClass());
		return obj;
	}
	
//	protected void resetBeanDefinition(String beanName) {
//		super.resetBeanDefinition(beanName);
//	}

}
