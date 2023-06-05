package com.github.jklasd.test.springcloud;

import java.util.Map;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

import com.github.jklasd.test.core.facade.scan.BeanCreaterScan;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;

public class LoadBalancerClientFactoryExt extends LoadBalancerClientFactory{
	
	public LoadBalancerClientFactoryExt() {
		BeanCreaterScan.getInstance().loadConfigurationClass(LoadBalancerClientConfiguration.class);
	}
	
	public <T> Map<String, T> getInstances(String name, Class<T> type) {
//		AnnotationConfigApplicationContext context = getContext(name);

		return BeanFactoryUtils.beansOfTypeIncludingAncestors(LazyApplicationContext.getInstance(), type);
	}
	
	public <T> T getInstance(String name, Class<T> type) {
//		AnnotationConfigApplicationContext context = getContext(name);
		try {
			return LazyApplicationContext.getInstance().getBean(type);
		}
		catch (NoSuchBeanDefinitionException e) {
			// ignore
		}
		return null;
	}
}
