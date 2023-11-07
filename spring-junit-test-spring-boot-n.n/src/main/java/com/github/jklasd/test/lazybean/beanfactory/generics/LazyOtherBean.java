package com.github.jklasd.test.lazybean.beanfactory.generics;

import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyOtherBean extends LazyAbstractPlugnBeanFactory{
	
	public static LazyAbstractPlugnBeanFactory getInstance() {
		return getInstanceByClass(LazyOtherBean.class);
	}
	
	@Override
	public String getName() {
		return "LazyOtherBean";
	}
	
	@Override
	public boolean isClassBean() {
		return true;
	}
	
	@Override
	public Integer getOrder() {
		
		return 500;
	}

	@Override
	public Object buildBean(BeanModel model) {
		try {
			Object obj = model.getTagClass().newInstance();
			return obj;
		} catch (InstantiationException | IllegalAccessException e) {
			log.error("LazyOtherBean",e);
		}
		return null;
	}

	@Override
	public boolean finded(BeanModel beanModel) {
		return ScanUtil.exists(beanModel.getTagClass());
	}

	@Override
	public void afterPropertiesSet(Object obj,BeanModel beanModel) {
		if(obj!=null) {
			BeanInitModel initModel = new BeanInitModel();
			initModel.setObj(obj);
			initModel.setTagClass(obj.getClass());
			initModel.setBeanName(beanModel.getBeanName());
    		LazyBean.getInstance().processAttr(initModel);// 递归注入代理对象
		}
	}

}
