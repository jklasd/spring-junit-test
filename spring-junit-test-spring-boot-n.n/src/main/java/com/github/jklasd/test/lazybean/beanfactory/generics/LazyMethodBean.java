package com.github.jklasd.test.lazybean.beanfactory.generics;

import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.model.JunitMethodDefinition;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;

public class LazyMethodBean implements LazyPlugnBeanFactory{

	@Override
	public Object buildBean(BeanModel model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean finded(BeanModel beanModel) {
		JunitMethodDefinition jmd = ScanUtil.findCreateBeanFactoryClass(beanModel);
		return jmd!=null;
	}

}
