package com.github.jklasd.test.lazybean.beanfactory;

import java.util.List;

import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyDubboBean;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyLocalGenericsBean;
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
	
	private List<LazyPlugnBeanFactory> beanFactorys = Lists.newArrayList(
			LazyMybatisMapperBean.getInstance(),
			LazyDubboBean.getInstance(),
			LazyLocalGenericsBean.getInstance()
			);
	
	public Object getTagertObjectCustom(BeanModel beanModel) {
		for(LazyPlugnBeanFactory lpbf : beanFactorys) {
			if(lpbf.finded(beanModel)) {
				return lpbf.buildBean(beanModel);
			}
		}
		return null;
	}
}
