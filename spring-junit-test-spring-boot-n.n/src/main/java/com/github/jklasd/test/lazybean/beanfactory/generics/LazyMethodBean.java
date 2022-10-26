package com.github.jklasd.test.lazybean.beanfactory.generics;

import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.model.JunitMethodDefinition;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.JavaBeanUtil;

public class LazyMethodBean extends LazyAbstractPlugnBeanFactory implements LazyPlugnBeanFactory{

	private ThreadLocal<JunitMethodDefinition> localCache = new ThreadLocal<>();
	
	@Override
	public Object buildBean(BeanModel model) {
		JunitMethodDefinition jmd = localCache.get();
		Object tagObj = JavaBeanUtil.getInstance().buildBean(jmd.getConfigurationClass(),jmd.getMethod(),model);
		return tagObj;
	}

	@Override
	public boolean finded(BeanModel beanModel) {
		JunitMethodDefinition jmd = ScanUtil.findCreateBeanFactoryClass(beanModel);
		if(jmd!=null) {
			localCache.set(jmd);
			return true;
		}
		return false;
	}

	@Override
	public void afterPropertiesSet(Object obj,BeanModel model) {
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
		return getInstanceByClass(LazyMethodBean.class);
	}
}
