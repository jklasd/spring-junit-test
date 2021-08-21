package com.github.jklasd.test.lazyplugn.spring;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.util.InvokeUtil;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyConfPropBind extends ConfigurationPropertiesBindingPostProcessor{
	private static ConfigurationPropertiesBindingPostProcessorExt lcpb = new ConfigurationPropertiesBindingPostProcessorExt();
	private static class ConfigurationPropertiesBindingPostProcessorExt{
		private ConfigurationPropertiesBindingPostProcessor process;
		//spring boot 1.5
		private Class<?> PropertiesConfigurationFactory = ScanUtil.loadClass("");
		//spring boot 2.0
		private Class<?> ConfigurationPropertiesBean = ScanUtil.loadClass("org.springframework.boot.context.properties.ConfigurationPropertiesBean");
		private Class<?> ConfigurationPropertiesBinder = ScanUtil.loadClass("org.springframework.boot.context.properties.ConfigurationPropertiesBinder");
		private Object processObj;
		private Map<String,Constructor<?>> cacheConstructor = Maps.newHashMap();
		public void postProcess(Object obj, ConfigurationProperties annotation){
			try {
			if(processObj==null) {
				if(PropertiesConfigurationFactory!=null) {//1.5
				}else if(ConfigurationPropertiesBean!=null) {//2.0
					Constructor<?> con = ConfigurationPropertiesBinder.getDeclaredConstructor(ApplicationContext.class);
					if(!con.isAccessible()) {
						con.setAccessible(true);
					}
					processObj = con.newInstance(TestUtil.getInstance().getApplicationContext());
					
				}else {
					log.error("=======未找到相应的ConfigurationPropertiesBindingPostProcessor处理类=======");
					return;
				}
			}
			
			if(PropertiesConfigurationFactory!=null) {//1.5
			}else if(ConfigurationPropertiesBean!=null) {//2.0
				String propBean = "ConfigurationPropertiesBean";
				if(!cacheConstructor.containsKey(propBean)) {
					cacheConstructor.put(propBean, ConfigurationPropertiesBean.getDeclaredConstructors()[0]);
					if(!cacheConstructor.get(propBean).isAccessible()) {
						cacheConstructor.get(propBean).setAccessible(true);
					}
				}
				Bindable<?> bind = Bindable.of(ResolvableType.forClass(obj.getClass()));
				Object configurationPropertiesBean = cacheConstructor.get(propBean).newInstance(null,obj,annotation,bind);
				InvokeUtil.invokeMethod(processObj, "bind", configurationPropertiesBean);
			}else {
				log.error("=======未找到相应的ConfigurationPropertiesBindingPostProcessor处理类=======");
				return;
			}
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void processConfigurationProperties(Object obj) {
		processConfigurationProperties(obj,obj.getClass().getAnnotation(ConfigurationProperties.class));
	}
//	private void postProcessBeforeInitialization(Object bean, String beanName,
//			ConfigurationProperties annotation) {
//		Object target = bean;
//		PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<Object>(
//				target);
//		factory.setPropertySources(this.propertySources);
//		factory.setApplicationContext(this.applicationContext);
//		factory.setValidator(determineValidator(bean));
//		// If no explicit conversion service is provided we add one so that (at least)
//		// comma-separated arrays of convertibles can be bound automatically
//		factory.setConversionService(this.conversionService == null
//				? getDefaultConversionService() : this.conversionService);
//		if (annotation != null) {
//			factory.setIgnoreInvalidFields(annotation.ignoreInvalidFields());
//			factory.setIgnoreUnknownFields(annotation.ignoreUnknownFields());
//			factory.setExceptionIfInvalid(annotation.exceptionIfInvalid());
//			factory.setIgnoreNestedProperties(annotation.ignoreNestedProperties());
//			if (StringUtils.hasLength(annotation.prefix())) {
//				factory.setTargetName(annotation.prefix());
//			}
//		}
//		try {
//			factory.bindPropertiesToTarget();
//		}
//		catch (Exception ex) {
//			String targetClass = ClassUtils.getShortName(target.getClass());
//			throw new BeanCreationException(beanName, "Could not bind properties to "
//					+ targetClass + " (" + getAnnotationDetails(annotation) + ")", ex);
//		}
//	}
	
	
	public static void processConfigurationProperties(Object obj, ConfigurationProperties annotation) {
		if(annotation == null) {
			return;
		}
		lcpb.postProcess(obj, annotation);
	}
}
