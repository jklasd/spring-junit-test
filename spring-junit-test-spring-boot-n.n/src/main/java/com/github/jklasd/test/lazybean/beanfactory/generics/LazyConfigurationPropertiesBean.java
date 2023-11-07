package com.github.jklasd.test.lazybean.beanfactory.generics;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.configprop.LazyConfPropBind;

public class LazyConfigurationPropertiesBean extends LazyAbstractPlugnBeanFactory{

	@Override
	public String getName() {
		return "LazyConfigurationPropertiesBean";
	}
	
	public Integer getOrder() {
		return 200;
	}
	
	@Override
	public boolean isClassBean() {
		return true;
	}
	
	@Override
	public Object buildBean(BeanModel beanModel) {
		return LazyBean.findCreateByProp(beanModel.getTagClass());
	}

	@Override
	public boolean finded(BeanModel beanModel) {
		ConfigurationProperties propConfig = (ConfigurationProperties) beanModel.getTagClass().getAnnotation(ConfigurationProperties.class);
		return propConfig!=null;
	}

	@Override
	public void afterPropertiesSet(Object obj,BeanModel beanModel) {
		if(obj!=null) {
			ConfigurationProperties propConfig = (ConfigurationProperties) beanModel.getTagClass().getAnnotation(ConfigurationProperties.class);
			LazyConfPropBind.processConfigurationProperties(obj,propConfig);
		}
	}
	
	public static LazyAbstractPlugnBeanFactory getInstance() {
		return getInstanceByClass(LazyConfigurationPropertiesBean.class);
	}

	
}
