package com.github.jklasd.test.lazyplugn.spring.configprop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.util.InvokeUtil;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

/**
 * spring boot 2.0
 * @author Administrator
 *
 */
@Slf4j
class PropBinder implements BinderHandler{
	private Class<?> ConfigurationPropertiesBean = ScanUtil.loadClass("org.springframework.boot.context.properties.ConfigurationPropertiesBean");
	private static Class<?> ConfigurationPropertiesBinder = ScanUtil.loadClass("org.springframework.boot.context.properties.ConfigurationPropertiesBinder");
	private Class<?> Bindable = ScanUtil.loadClass("org.springframework.boot.context.properties.bind.Bindable");
	private Map<String,Constructor<?>> cacheConstructor = Maps.newHashMap();
	private Object processObj;
	
	public static BinderHandler getHandler() {
		if(ConfigurationPropertiesBinder!=null) {
			return new PropBinder();
		}
		return null;
	}
	
	@Override
	public void postProcess(Object obj, ConfigurationProperties annotation) {
		try {
			if(processObj == null) {
				Constructor<?> con = ConfigurationPropertiesBinder.getDeclaredConstructor(ApplicationContext.class);
				if(!con.isAccessible()) {
					con.setAccessible(true);
				}
				processObj = con.newInstance(TestUtil.getInstance().getApplicationContext());
			}
			
			String propBean = "ConfigurationPropertiesBean";
			if(!cacheConstructor.containsKey(propBean)) {
				cacheConstructor.put(propBean, ConfigurationPropertiesBean.getDeclaredConstructors()[0]);
				if(!cacheConstructor.get(propBean).isAccessible()) {
					cacheConstructor.get(propBean).setAccessible(true);
				}
			}
			Method method = Bindable.getDeclaredMethod("of", ResolvableType.class);
			Object bindTarget = method.invoke(null, ResolvableType.forClass(obj.getClass()));
			Method withAnnotations = Bindable.getDeclaredMethod("withAnnotations", ConfigurationProperties.class);
			withAnnotations.invoke(bindTarget, annotation);
			if (obj != null) {
				InvokeUtil.invokeMethod(processObj, "withExistingValue", obj);
			}
			Object configurationPropertiesBean = cacheConstructor.get(propBean)
					.newInstance(null,obj,annotation,bindTarget);
			InvokeUtil.invokeMethod(processObj, "bind", configurationPropertiesBean);
		} catch (Exception e) {
			log.error("绑定prop异常",e);
		}
	}
}
