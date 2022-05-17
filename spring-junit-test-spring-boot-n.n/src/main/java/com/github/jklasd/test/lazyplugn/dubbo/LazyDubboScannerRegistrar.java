package com.github.jklasd.test.lazyplugn.dubbo;

import com.github.jklasd.test.common.interf.register.ScannerRegistrarI;

public class LazyDubboScannerRegistrar implements ScannerRegistrarI {

	@Override
	public boolean using() {
		return LazyDubboBean.useDubbo();
	}

	@Override
	public void scannerAndRegister() {
//		List<Class<?>> dubboServiceList = ScanUtil.findClassWithAnnotation(LazyDubboBean.getAnnotionClass(),ClassScan.getApplicationAllClassMap());
		
		//影响
//		LazyDubboAnnotationRefHandler
	}

}
