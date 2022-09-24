package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.util.List;

import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;

public class LazyLocalGenericsBean implements LazyPlugnBeanFactory{
	
	private ThreadLocal<Object> localCache = new ThreadLocal<>();

	@Override
	public Object buildBean(BeanModel model) {
		if(localCache.get()!=null) {
			/**
			 * 开始创建对象
			 */
			return LazyListableBeanFactory.getInstance().getBean(model.getBeanName(), model.getTagClass());
		}
		return null;
	}

	@Override
	public boolean finded(BeanModel beanModel) {
		
		/**
		 * 如果在本地找到实现类
		 */
		if(beanModel.getTagClass().isInterface()) {
			List<Class<?>> impleList = ScanUtil.findClassImplInterface(beanModel.getTagClass());
			if(!impleList.isEmpty()) {
				localCache.set(impleList);
				return true;
			}
		}
		localCache.remove();
		return false;
	}

}
