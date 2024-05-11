package com.github.jklasd.test.lazybean.beanfactory.generics;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.interf.register.ConfigurationPropertiesI;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

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
		return ContainerManager.getComponent(ConfigurationPropertiesI.class)!=null;
	}

	@Override
	public void afterPropertiesSet(Object obj,BeanModel beanModel) {
		if(obj!=null) {
			ContainerManager.execu(ConfigurationPropertiesI.class, cpi->{
				((ConfigurationPropertiesI)cpi).processConfigurationProperties(obj,beanModel.getTagClass());
			});
//			ContainerManager.getComponent(ConfigurationPropertiesI.class);
//			ConfigurationProperties propConfig = (ConfigurationProperties) beanModel.getTagClass().getAnnotation(ConfigurationProperties.class);
//			LazyConfPropBind.processConfigurationProperties(obj,propConfig);
		}
	}
	
	public static LazyAbstractPlugnBeanFactory getInstance() {
		return getInstanceByClass(LazyConfigurationPropertiesBean.class);
	}

	
}
