package com.github.jklasd.test.lazybean.beanfactory;

import java.util.List;

import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyDubboBean;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyLocalGenericsBean;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyMethodBean;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyMybatisMapperBean;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.google.common.collect.Lists;

public class LazyPlugnBeanFactoryManager {

	static LazyPlugnBeanFactoryManager instance;
	public final static LazyPlugnBeanFactoryManager getInstance() {
		if(instance != null) {
			return instance;
		}
		synchronized (LazyPlugnBeanFactoryManager.class) {
			if(instance == null) {
				instance = new LazyPlugnBeanFactoryManager();
			}
		}
		return instance;
	}
	
	private List<LazyPlugnBeanFactory> interfaceBeanFactorys = Lists.newArrayList(
			LazyMybatisMapperBean.getInstance(),
			LazyDubboBean.getInstance(),
			LazyLocalGenericsBean.getInstance(),
			LazyMethodBean.getInstance()
			);
	
	public Object getTagertObjectCustomForInterface(BeanModel beanModel) {
		for(LazyPlugnBeanFactory lpbf : interfaceBeanFactorys) {
			if(lpbf.finded(beanModel)) {
				Object obj = lpbf.buildBean(beanModel);
				lpbf.afterPropertiesSet(obj,beanModel);
				return obj;
			}
		}
		return null;
	}
	
	private List<LazyPlugnBeanFactory> classBeanFactorys = Lists.newArrayList(
			LazyLocalGenericsBean.getInstance(),
			LazyMethodBean.getInstance()
			);
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
