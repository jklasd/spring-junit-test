package com.github.jklasd.test.springboot.configuration.configprop;

import org.springframework.boot.context.properties.ConfigurationProperties;

public interface BinderHandler {
	void postProcess(Object obj, ConfigurationProperties annotation);

	void postProcess(ConfigurationModel model);
}
