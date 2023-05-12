package com.github.jklasd.test.common.component;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.interf.handler.LazyPlugnBeanFactory;
import com.github.jklasd.test.common.interf.register.ObjectProviderBuilder;
import com.github.jklasd.test.common.model.BeanModel;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class LazyPlugnBeanFactoryComponent extends AbstractComponent{
	static Map<String,LazyPlugnBeanFactory> cacheFactory = Maps.newHashMap();
	@Override
	<T> void add(T component) {
		LazyPlugnBeanFactory lazyPlugn = (LazyPlugnBeanFactory) component;
		
		cacheFactory.put(lazyPlugn.getName(), lazyPlugn);
	}

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
	
	private List<String> interfaceBeanFactorys = Lists.newArrayList(
			"LazyMybatisMapperBean",
			"LazyDubboBean",
			"LazyLocalGenericsBean",
			"LazyMethodBean");
	public Object getTagertObjectCustomForInterface(BeanModel beanModel) {
		for(String name : interfaceBeanFactorys) {
			LazyPlugnBeanFactory lpbf = cacheFactory.get(name);
			if(lpbf == null) {
				continue;
			}
			if(lpbf.finded(beanModel)) {
				Object obj = lpbf.buildBean(beanModel);
				lpbf.afterPropertiesSet(obj,beanModel);
				return obj;
			}
		}
		if(ObjectProvider.class == beanModel.getTagClass()) {
			return ((ObjectProviderBuilder)ContainerManager.getComponent(ObjectProviderBuilder.class.getName()))
					.buildObjectProviderImpl((Class<?>)beanModel.getClassGeneric()[0]);
		}
		return null;
	}
	
	private List<String> classBeanFactorys = Lists.newArrayList(
			"LazyBeanDefBean",
			"LazyConfigurationPropertiesBean",
			"LazyMethodBean",
			"LazyOtherBean"); 
	public Object getTagertObjectCustomForClass(BeanModel beanModel) {
		for(String name : classBeanFactorys) {
			LazyPlugnBeanFactory lpbf = cacheFactory.get(name);
			if(lpbf == null) {
				continue;
			}
			if(lpbf.finded(beanModel)) {
				Object obj = lpbf.buildBean(beanModel);
				lpbf.afterPropertiesSet(obj,beanModel);
				return obj;
			}
		}
		return null;
	}
}
