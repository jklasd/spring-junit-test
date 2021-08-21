package com.github.jklasd.test.lazyplugn.spring.configprop;

import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.util.ScanUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * spring boot 1.5
 * @author Administrator
 *
 */
@Slf4j
class PropFactory implements BinderHandler{
	private static Class<?> PropertiesConfigurationFactory = ScanUtil.loadClass("");
	public static BinderHandler getHandler() {
		if(PropertiesConfigurationFactory!=null) {
			return new PropFactory();
		}
		return null;
	}
	@Override
	public void postProcess(Object obj, ConfigurationProperties annotation) {
		PropertiesConfigurationFactory<Object> factory = 
				new PropertiesConfigurationFactory<Object>(obj);
		factory.setPropertySources(TestUtil.getInstance().getPropertySource());
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
