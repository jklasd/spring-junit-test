package com.github.jklasd.test.lazyplugn.spring.configprop;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.validation.annotation.Validated;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.abstrac.JunitApplicationContext;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.util.ScanUtil;
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
			Class<?>[] paramTypes = new Class<?>[] {ApplicationContext.class,Object.class,String.class};//ApplicationContext applicationContext, Object bean, String beanName
			Object configurationPropertiesBean = JunitInvokeUtil.invokeStaticMethod(ConfigurationPropertiesBean, "get",paramTypes ,applicationContext,obj,obj.getClass().getName());
			if(configurationPropertiesBean == null) {//注解不在类上，需要传递进去
				String propBean = "ConfigurationPropertiesBean";
				if(!cacheConstructor.containsKey(propBean)) {
					cacheConstructor.put(propBean, ConfigurationPropertiesBean.getDeclaredConstructors()[0]);
					if(!cacheConstructor.get(propBean).isAccessible()) {
						cacheConstructor.get(propBean).setAccessible(true);
					}
				}
				
				Annotation[] annotations = new Annotation[] { annotation };
				ResolvableType bindType = ResolvableType.forClass(obj.getClass());
				Bindable<Object> bindTarget = Bindable.of(bindType).withAnnotations(annotations);
				if (obj != null) {
					bindTarget = bindTarget.withExistingValue(obj);
				}
				
				configurationPropertiesBean = cacheConstructor.get(propBean)
						.newInstance(obj.getClass().getName(),obj,annotation,bindTarget);
			}
			JunitInvokeUtil.invokeMethod(processObj, "bind", configurationPropertiesBean);
		} catch (Exception e) {
			log.error("绑定prop异常",e);
			throw new JunitException(e, true);
		}
	}
	private static <A extends Annotation> MergedAnnotation<A> findMergedAnnotation(AnnotatedElement element,
			Class<A> annotationType) {
		return (element != null) ? MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY).get(annotationType)
				: MergedAnnotation.missing();
	}

	private synchronized void buildProcessObj()
			throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if(processObj==null) {
			
			BeanFactoryPostProcessor postProcessor = (BeanFactoryPostProcessor) PropertySourcesPlaceholderConfigurer.newInstance();
			EnvironmentAware aware = (EnvironmentAware) postProcessor;
			aware.setEnvironment(applicationContext.getEnvironment());
			postProcessor.postProcessBeanFactory(applicationContext.getBeanFactory());
			
//			applicationContext.registProxyBean("propertySourcesPlaceholderConfigurer", postProcessor, PropertySourcesPlaceholderConfigurer);
			applicationContext.getDefaultListableBeanFactory().registerSingleton("propertySourcesPlaceholderConfigurer", postProcessor);
			
			Constructor<?> con = ConfigurationPropertiesBinder.getDeclaredConstructor(ApplicationContext.class);
			if(!con.isAccessible()) {
				con.setAccessible(true);
			}
			processObj = con.newInstance(applicationContext);
		}
	}

	@Override
	public void postProcess(ConfigurationModel model) {
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
			Object obj = model.getObj();
			MergedAnnotation<ConfigurationProperties> merageAnnotation = MergedAnnotation.missing();
			if (model.getMethod() != null) {
				merageAnnotation = findMergedAnnotation(model.getMethod(), ConfigurationProperties.class);
			}
			if (!merageAnnotation.isPresent()) {
				merageAnnotation = findMergedAnnotation(obj.getClass(), ConfigurationProperties.class);
			}
			ConfigurationProperties annotation = merageAnnotation.synthesize();
			
			Annotation[] annotations = new Annotation[] { annotation };
			ResolvableType bindType = ResolvableType.forClass(obj.getClass());
			Bindable<Object> bindTarget = Bindable.of(bindType).withAnnotations(annotations);
			if (obj != null) {
				bindTarget = bindTarget.withExistingValue(obj);
			}
			
			Object configurationPropertiesBean = cacheConstructor.get(propBean)
					.newInstance(obj.getClass().getName(),obj,annotation,bindTarget);
			JunitInvokeUtil.invokeMethod(processObj, "bind", configurationPropertiesBean);
		} catch (Exception e) {
			log.error("绑定prop异常",e);
			throw new JunitException(e, true);
		}
	}
}
