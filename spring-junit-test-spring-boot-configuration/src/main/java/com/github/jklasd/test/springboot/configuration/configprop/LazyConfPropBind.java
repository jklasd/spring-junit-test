package com.github.jklasd.test.springboot.configuration.configprop;

import java.lang.reflect.Method;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.github.jklasd.test.common.interf.register.ConfigurationPropertiesI;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyConfPropBind implements ConfigurationPropertiesI{
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
		
		public void postProcess(ConfigurationModel model){
			handler.postProcess(model);
		}
	}
	
	public void processConfigurationProperties(ConfigurationModel model) {
		lcpb.postProcess(model);
	}
	
	@Override
	public void processConfigurationProperties(Object obj) {
		processConfigurationProperties(obj,obj.getClass());
	}
	
	@Override
	public void processConfigurationProperties(Object obj,Class<?> superClass) {
		ConfigurationProperties annotation = superClass.getAnnotation(ConfigurationProperties.class);
		if(annotation == null) {
			return;
		}
		lcpb.postProcess(obj, annotation);
	}

	@Override
	public void processConfigurationProperties(Method method, Object tagObj) {
		ConfigurationProperties prop = null;
    	if((prop = method.getAnnotation(ConfigurationProperties.class))!=null
    			|| (prop = tagObj.getClass().getAnnotation(ConfigurationProperties.class))!=null) {
    		ConfigurationModel cm = new ConfigurationModel();
    		cm.setMethod(method);
    		cm.setObj(tagObj);
    		cm.setProp(prop);
    		processConfigurationProperties(cm);
    	}
	}
	
	@Override
	public String getBeanKey() {
		return ConfigurationPropertiesI.class.getName();
	}
}
