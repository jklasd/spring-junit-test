package com.github.jklasd.test.lazyplugn.dubbo;

import com.github.jklasd.test.common.interf.register.ScannerRegistrarI;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyDubboBean;

public class LazyDubboScannerRegistrar implements ScannerRegistrarI {

	@Override
	public boolean using() {
		return LazyDubboBean.useDubbo();
	}

	//服务暴露时能用到处理
	@Override
	public void scannerAndRegister() {
//		List<Class<?>> dubboServiceList = ScanUtil.findClassWithAnnotation(LazyDubboBean.getAnnotionClass(),ClassScan.getApplicationAllClassMap());
		
		//影响
//		LazyDubboAnnotationRefHandler
	}

}
