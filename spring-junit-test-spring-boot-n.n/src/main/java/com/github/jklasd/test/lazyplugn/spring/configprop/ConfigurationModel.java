package com.github.jklasd.test.lazyplugn.spring.configprop;

import java.lang.reflect.AnnotatedElement;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
public class ConfigurationModel {

	private Object obj;
	private AnnotatedElement method;
	private ConfigurationProperties prop;
	
}
