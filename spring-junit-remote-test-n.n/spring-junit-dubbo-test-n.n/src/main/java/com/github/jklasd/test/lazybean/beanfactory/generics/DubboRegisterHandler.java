package com.github.jklasd.test.lazybean.beanfactory.generics;

import com.github.jklasd.test.common.interf.handler.RegisterHandler;

public class DubboRegisterHandler implements RegisterHandler{

	@Override
	public String getRegisterKey() {
		return "DubboRegisterHandler";
	}

	@Override
	public void registClass(Class<?> rClass) {
		LazyDubboBean.getInstance().registerDubboService(rClass);
	}

}
