package com.github.jklasd.test.lazyplugn.dubbo.reference;

public class ApacheDubboReferenceHandler extends AbstractReferenceHandler{

	@Override
	public String getType() {
		return "org.apache.dubbo.config.annotation.Reference";
	}
}
