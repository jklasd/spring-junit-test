package com.github.jklasd.test.lazyplugn.spring;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import com.github.jklasd.test.TestUtil;

public class ConditionContextImpl implements ConditionContext{

	@Override
	public BeanDefinitionRegistry getRegistry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() {
		// TODO Auto-generated method stub
		return TestUtil.getInstance().getApplicationContext().getBeanFactory();
	}

	@Override
	public Environment getEnvironment() {
		return TestUtil.getInstance().getApplicationContext().getEnvironment();
	}

	@Override
	public ResourceLoader getResourceLoader() {
		// TODO Auto-generated method stub
		return TestUtil.getInstance().getApplicationContext();
	}

	@Override
	public ClassLoader getClassLoader() {
		return ConditionContextImpl.class.getClassLoader();
	}

}
