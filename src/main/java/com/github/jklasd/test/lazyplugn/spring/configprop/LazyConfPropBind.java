package com.github.jklasd.test.lazyplugn.spring.configprop;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyConfPropBind{
	private static ConfigurationPropertiesBindingPostProcessorExt lcpb = new ConfigurationPropertiesBindingPostProcessorExt();
	private static class ConfigurationPropertiesBindingPostProcessorExt{
		private BinderHandler handler;
		{
			handler = PropFactory.getBinderHandler();
			if(handler == null) {
				handler = PropBinder.getBinderHandler();
			}
		}
		public void postProcess(Object obj, ConfigurationProperties annotation){
			handler.postProcess(obj, annotation);
		}
	}
	
	public static void processConfigurationProperties(Object obj) {
		processConfigurationProperties(obj,obj.getClass().getAnnotation(ConfigurationProperties.class));
	}
	
	public static void processConfigurationProperties(Object obj, ConfigurationProperties annotation) {
		if(annotation == null) {
			return;
		}
		lcpb.postProcess(obj, annotation);
	}
}
