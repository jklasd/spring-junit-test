package com.github.jklasd.test.core.common.register;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.interf.handler.RegisterHandler;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.lazyplugn.spring.JavaBeanUtil;

public class ConfigurationRegisterHandler implements RegisterHandler{

	@Override
	public String getRegisterKey() {
		return "ConfigurationRegisterHandler";
	}

	/**
	 * 指定某个configural Class 实例化
	 * 因为有一些开发人员编写的创建bean方法，可能并没有被依赖上，导致单元测试工具运行时不被执行
	 */
	@Override
	public void registClass(Class<?> configuralClass) {
		if(!ClassScan.getInstance().isInScanPath(configuralClass)) {
			return;
		}
		
		//注册className
		Method[] mds = configuralClass.getMethods();
		for(Method m : mds) {
			BeanModel bm = new BeanModel();
			bm.setTagClass(m.getReturnType());
			
			JavaBeanUtil.getInstance().buildBean(configuralClass, m, bm);
		}
	}

}
