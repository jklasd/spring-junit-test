package com.github.jklasd.test.common.component;

import java.util.List;

import com.github.jklasd.test.common.interf.register.LazyPlugnBeanFactory;
import com.github.jklasd.test.common.model.BeanModel;

public class LazyPlugnBeanFactoryComponent {

	static LazyPlugnBeanFactoryComponent instance;
	public final static LazyPlugnBeanFactoryComponent getInstance() {
		if(instance != null) {
			return instance;
		}
		synchronized (LazyPlugnBeanFactoryComponent.class) {
			if(instance == null) {
				instance = new LazyPlugnBeanFactoryComponent();
			}
		}
		return instance;
	}
	
	private List<LazyPlugnBeanFactory> interfaceBeanFactorys = null;
	
	
	private List<LazyPlugnBeanFactory> classBeanFactorys = null; 
//			Lists.newArrayList(
//			LazyBeanDefBean.getInstance(),
//			LazyConfigurationPropertiesBean.getInstance(),
//			LazyMethodBean.getInstance(),
//			LazyOtherBean.getInstance()
//			);
	public Object getTagertObjectCustomForClass(BeanModel beanModel) {
		for(LazyPlugnBeanFactory lpbf : classBeanFactorys) {
			if(lpbf.finded(beanModel)) {
				Object obj = lpbf.buildBean(beanModel);
				lpbf.afterPropertiesSet(obj,beanModel);
				return obj;
			}
		}
		return null;
	}
}
