package com.github.jklasd.test.lazybean.beanfactory.generics;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory.RootBeanDefinitionBuilder;

public class LazyBeanDefBean extends LazyAbstractPlugnBeanFactory{

	private ThreadLocal<BeanDefinition> localCache = new ThreadLocal<>();
	
	LazyListableBeanFactory beanFactory = LazyListableBeanFactory.getInstance();
	
	@Override
	public Object buildBean(BeanModel model) {
		
		return beanFactory.doCreateBean(model.getBeanName(), RootBeanDefinitionBuilder.build(localCache.get()), null);
	}

	@Override
	public boolean finded(BeanModel beanModel) {
		BeanDefinition bd = null;
		if(beanFactory.containsBeanDefinition(beanModel.getBeanName())) {
			bd = beanFactory.getBeanDefinition(beanModel.getBeanName());
			if(bd!=null) {
				AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
				if(!beanModel.getTagClass().isAssignableFrom(abd.getBeanClass())) {
					bd = null;
				}
			}
		}
		if(bd == null) {
			bd = beanFactory.getFirstBeanDefinition(beanModel.getTagClass());
			if(bd!=null) {
				beanModel.setBeanName(beanModel.getTagClass().getName());//重置beanName
				beanFactory.registerBeanDefinition(beanModel.getBeanName(), bd);
			}
		}
		localCache.set(bd);
		
		return bd != null;
	}

	@Override
	public void afterPropertiesSet(Object obj, BeanModel model) {
		if(obj!=null) {
			BeanInitModel initModel = new BeanInitModel();
			initModel.setObj(obj);
			initModel.setTagClass(obj.getClass());
			initModel.setBeanName(model.getBeanName());
    		LazyBean.getInstance().processAttr(initModel);// 递归注入代理对象
		}
		localCache.remove();
	}
	
	
	public static LazyAbstractPlugnBeanFactory getInstance() {
		return getInstanceByClass(LazyBeanDefBean.class);
	}
}
