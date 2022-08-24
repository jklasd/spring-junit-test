package com.github.jklasd.test.lazyplugn.spring.configprop;

import org.springframework.boot.context.properties.ConfigurationProperties;

public interface BinderHandler {
	void postProcess(Object obj, ConfigurationProperties annotation);
}
