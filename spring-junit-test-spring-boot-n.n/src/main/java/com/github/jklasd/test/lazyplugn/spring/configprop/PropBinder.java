package com.github.jklasd.test.lazyplugn.spring.configprop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.abstrac.JunitApplicationContext;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.github.jklasd.test.util.JunitInvokeUtil;
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
	private Class<?> PropertySourcesPlaceholderConfigurer = ScanUtil.loadClass("org.springframework.context.support.PropertySourcesPlaceholderConfigurer");
	private Class<?> BindableC = ScanUtil.loadClass("org.springframework.boot.context.properties.bind.Bindable");
	private Map<String,Constructor<?>> cacheConstructor = Maps.newHashMap();
	private Object processObj;
	private JunitApplicationContext applicationContext;
	public static BinderHandler getBinderHandler() {
		if(ConfigurationPropertiesBean!=null) {
			PropBinder signBean = new PropBinder();
			signBean.applicationContext= TestUtil.getInstance().getApplicationContext();
			return signBean;
		}
		return null;
	}
	
	@Override
	public void postProcess(Object obj, ConfigurationProperties annotation) {
		try {
			if(processObj == null) {
				buildProcessObj();
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

	private synchronized void buildProcessObj()
			throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if(processObj==null) {
			
			Constructor<?> con = ConfigurationPropertiesBinder.getDeclaredConstructor(ApplicationContext.class);
			if(!con.isAccessible()) {
				con.setAccessible(true);
			}
			BeanFactoryPostProcessor postProcessor = (BeanFactoryPostProcessor) PropertySourcesPlaceholderConfigurer.newInstance();
			EnvironmentAware aware = (EnvironmentAware) postProcessor;
			aware.setEnvironment(applicationContext.getEnvironment());
			postProcessor.postProcessBeanFactory(applicationContext.getBeanFactory());
			
//			applicationContext.registProxyBean("propertySourcesPlaceholderConfigurer", postProcessor, PropertySourcesPlaceholderConfigurer);
			
			processObj = con.newInstance(applicationContext);
		}
	}
}
