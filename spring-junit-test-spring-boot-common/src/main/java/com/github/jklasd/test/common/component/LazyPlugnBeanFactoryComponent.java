package com.github.jklasd.test.common.component;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.interf.handler.LazyPlugnBeanFactory;
import com.github.jklasd.test.common.interf.register.ObjectProviderBuilder;
import com.github.jklasd.test.common.model.BeanModel;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class LazyPlugnBeanFactoryComponent extends AbstractComponent{
	
	static Set<String> cacheName = Sets.newConcurrentHashSet();
	
	@Override
	<T> void add(T component) {
		LazyPlugnBeanFactory lazyPlugn = (LazyPlugnBeanFactory) component;
		
		if(cacheName.contains(lazyPlugn.getName()) ||
				!lazyPlugn.canBeInstance()) {
			return;
		}
		cacheName.add(lazyPlugn.getName());
		
		if(lazyPlugn.isClassBean()) {
			classBeanFactorys.add(lazyPlugn);
			classBeanFactorys.sort(new Comparator<LazyPlugnBeanFactory>() {
				@Override
				public int compare(LazyPlugnBeanFactory o1, LazyPlugnBeanFactory o2) {
					return o1.getOrder()-o2.getOrder();
				}
			});
		}
		
		if(lazyPlugn.isInterfaceBean()) {
			interfaceBeanFactorys.add(lazyPlugn);
			interfaceBeanFactorys.sort(new Comparator<LazyPlugnBeanFactory>() {
				@Override
				public int compare(LazyPlugnBeanFactory o1, LazyPlugnBeanFactory o2) {
					return o1.getOrder()-o2.getOrder();
				}
			});
		}
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
	
	private static List<LazyPlugnBeanFactory> interfaceBeanFactorys = Lists.newArrayList();
	public Object getTagertObjectCustomForInterface(BeanModel beanModel) {
		for(LazyPlugnBeanFactory lpbf : interfaceBeanFactorys) {
			if(lpbf.finded(beanModel)) {
				Object obj = lpbf.buildBean(beanModel);
				lpbf.afterPropertiesSet(obj,beanModel);
				return obj;
			}
		}
		if(ObjectProvider.class == beanModel.getTagClass()) {
			Type type = beanModel.getClassGeneric()[0];
			ObjectProviderBuilder builder = (ObjectProviderBuilder)ContainerManager.getComponent(ObjectProviderBuilder.class.getName());
			return builder
					.buildObjectProviderImpl(type);
		}
		return null;
	}
	
	private static List<LazyPlugnBeanFactory> classBeanFactorys = Lists.newArrayList(); 
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
