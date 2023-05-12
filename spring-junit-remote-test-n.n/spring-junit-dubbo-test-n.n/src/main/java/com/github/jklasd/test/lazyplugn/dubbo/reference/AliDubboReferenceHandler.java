package com.github.jklasd.test.lazyplugn.dubbo.reference;

public class AliDubboReferenceHandler extends AbstractReferenceHandler{

	@Override
	public String getType() {
		return "com.alibaba.dubbo.config.annotation.Reference";
	}
	
}
