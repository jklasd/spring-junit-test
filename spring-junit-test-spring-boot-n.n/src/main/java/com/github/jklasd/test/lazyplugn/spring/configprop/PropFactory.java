package com.github.jklasd.test.lazyplugn.spring.configprop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.PropertySources;
import org.springframework.util.StringUtils;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.ScanUtil;
import com.github.jklasd.test.exception.JunitException;

import lombok.extern.slf4j.Slf4j;

/**
 * spring boot 1.5
 * @author Administrator
 *
 */
@Slf4j
class PropFactory implements BinderHandler{
	private static Class<?> PropertiesConfigurationFactory = ScanUtil.loadClass("org.springframework.boot.bind.PropertiesConfigurationFactory");
	public static BinderHandler getBinderHandler() {
		if(PropertiesConfigurationFactory!=null) {
			log.info("使用PropertiesConfigurationFactory绑定prop");
			return new PropFactory();
		}
		return null;
	}
	@Override
	public void postProcess(Object obj, ConfigurationProperties annotation) {
		try {
			Object factory = PropertiesConfigurationFactory.getDeclaredConstructors()[0].newInstance(obj);
			
			Method setPropertySources = PropertiesConfigurationFactory.getDeclaredMethod("setPropertySources", PropertySources.class);
			setPropertySources.invoke(factory, TestUtil.getInstance().getPropertySource());
			
			Method setIgnoreInvalidFields = PropertiesConfigurationFactory.getDeclaredMethod("setIgnoreInvalidFields", boolean.class);
			setIgnoreInvalidFields.invoke(factory, annotation.ignoreInvalidFields());
			
			Method setIgnoreUnknownFields = PropertiesConfigurationFactory.getDeclaredMethod("setIgnoreUnknownFields", boolean.class);
			setIgnoreUnknownFields.invoke(factory, annotation.ignoreUnknownFields());
			
//			Method setIgnoreNestedProperties = PropertiesConfigurationFactory.getDeclaredMethod("setIgnoreNestedProperties", boolean.class);
//			setIgnoreNestedProperties.invoke(factory, annotation.ignoreNestedProperties());
			
			Method setTargetName = PropertiesConfigurationFactory.getDeclaredMethod("setTargetName", String.class);
			if (StringUtils.hasLength(annotation.prefix())) {
				setTargetName.invoke(factory, annotation.prefix());
			}
			
			Method bindPropertiesToTarget = PropertiesConfigurationFactory.getDeclaredMethod("bindPropertiesToTarget");
			bindPropertiesToTarget.invoke(factory);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException 
				| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
			throw new JunitException("构建PropFactory异常");
		}
//		factory.setPropertySources(TestUtil.getInstance().getPropertySource());
//		factory.setIgnoreInvalidFields(annotation.ignoreInvalidFields());
//		factory.setIgnoreUnknownFields(annotation.ignoreUnknownFields());
//		factory.setIgnoreNestedProperties(annotation.ignoreNestedProperties());
//		if (StringUtils.hasLength(annotation.prefix())) {
//			factory.setTargetName(annotation.prefix());
//		}
//		try {
//			factory.bindPropertiesToTarget();
//		}
//		catch (Exception ex) {
//			log.error("Could not bind properties to "+obj.getClass()+" (" + annotation + ")",ex);
//		}
	}
}
