package com.github.jklasd.test.spring;

import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import com.github.jklasd.test.TestUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyConfigurationPropertiesBindingPostProcessor {

	public static void processConfigurationProperties(Object obj) {
		processConfigurationProperties(obj,obj.getClass().getAnnotation(ConfigurationProperties.class));
	}
	public static void processConfigurationProperties(Object obj, ConfigurationProperties annotation) {
		if(annotation == null) {
			return;
		}
		Object target = obj;
		PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<Object>(
				target);
		factory.setPropertySources(TestUtil.getInstance().getPropertySource());
//		factory.setValidator(determineValidator(bean));
		// If no explicit conversion service is provided we add one so that (at least)
		// comma-separated arrays of convertibles can be bound automatically
//		factory.setConversionService(this.conversionService == null
//				? getDefaultConversionService() : this.conversionService);
		factory.setIgnoreInvalidFields(annotation.ignoreInvalidFields());
		factory.setIgnoreUnknownFields(annotation.ignoreUnknownFields());
		factory.setIgnoreNestedProperties(annotation.ignoreNestedProperties());
		if (StringUtils.hasLength(annotation.prefix())) {
			factory.setTargetName(annotation.prefix());
		}
		try {
			factory.bindPropertiesToTarget();
		}
		catch (Exception ex) {
			log.error("Could not bind properties to "+obj.getClass()+" (" + annotation + ")",ex);
		}
	}

}
