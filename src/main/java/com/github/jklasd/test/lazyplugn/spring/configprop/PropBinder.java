package com.github.jklasd.test.lazyplugn.spring.configprop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.util.JunitInvokeUtil;
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
	private static Class<?> ConfigurationPropertiesBean = ScanUtil.loadClass("org.springframework.boot.context.properties.ConfigurationPropertiesBean");
	private Class<?> ConfigurationPropertiesBinder = ScanUtil.loadClass("org.springframework.boot.context.properties.ConfigurationPropertiesBinder");
	private Class<?> BindableC = ScanUtil.loadClass("org.springframework.boot.context.properties.bind.Bindable");
	private Map<String,Constructor<?>> cacheConstructor = Maps.newHashMap();
	private Object processObj;
	
	public static BinderHandler getBinderHandler() {
		if(ConfigurationPropertiesBean!=null) {
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
			
//			Bindable<Object> bindTarget = Bindable.ofInstance(obj)
//					.withAnnotations(new Annotation[] {annotation});
			Object bindTarget = null;
			Method withAnnotations = null;
			for(Method m : BindableC.getDeclaredMethods()) {
				if(m.getName().equals("ofInstance")) {
					bindTarget = m.invoke(null, obj);
				}else if(m.getName().equals("withAnnotations")) {
					withAnnotations = m;
				}
			}
			Object anns = new Annotation[] {annotation};
			bindTarget = withAnnotations.invoke(bindTarget, anns);
			Object configurationPropertiesBean = cacheConstructor.get(propBean)
					.newInstance(null,obj,annotation,bindTarget);
			JunitInvokeUtil.invokeMethod(processObj, "bind", configurationPropertiesBean);
		} catch (Exception e) {
			log.error("绑定prop异常",e);
		}
	}
}
