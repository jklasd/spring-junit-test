package com.github.jklasd.test.lazyplugn.spring;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.EventPublishingRunListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.abstrac.JunitApplicationContext;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.beanfactory.LazyProxyManager;
import com.github.jklasd.test.util.BeanNameUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyApplicationContext extends JunitApplicationContext{
	
	@Getter
	private LazyListableBeanFactory lazyBeanFactory;
	private LazyApplicationContext() {
		super(LazyListableBeanFactory.getInstance());
		lazyBeanFactory = (LazyListableBeanFactory) getDefaultListableBeanFactory();
	}
	private static LazyApplicationContext signBean = new LazyApplicationContext();
	public static LazyApplicationContext getInstance() {
		return signBean;
	}
	
	Map<Class,Map> cacheTypeBeanMap = Maps.newHashMap();
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type){
		if(cacheTypeBeanMap.containsKey(type)) {
			return cacheTypeBeanMap.get(type);
		}
		Map<String, T> map = Maps.newHashMap();
		List<Class<?>> subClass = ScanUtil.findSubClass(type);
		subClass.forEach(subC ->{
			if(subC == null) {
				log.error("subC=>{},type->{}",subC,type);
			}
			Object bean = LazyBean.getInstance().buildProxy(subC);
			if(bean!=null) {
				map.put(subC.getName(), (T) bean);
			}else {
				log.warn("获取bean集合，出现问题");
			}
		});
		cacheTypeBeanMap.put(type,map);
		return map;
	}
	
	Map<String,Object> cacheProxyBean = Maps.newHashMap();
	
	public Object getBean(String beanName) {
		if(cacheProxyBean.containsKey(beanName)) {
			return cacheProxyBean.get(beanName);
		}
		Object tmpObject = lazyBeanFactory.getBean(beanName);
		if(tmpObject != null) {
			return tmpObject;
		}
		BeanDefinition beanDef = lazyBeanFactory.getBeanDefinition(beanName);
		if(beanDef != null && beanDef instanceof AbstractBeanDefinition) {
			AbstractBeanDefinition tmpBeanDef = (AbstractBeanDefinition) beanDef;
			if(!tmpBeanDef.hasBeanClass()) {
				tmpBeanDef.setBeanClass(ScanUtil.loadClass(tmpBeanDef.getBeanClassName()));
			}
			Object bean = LazyBean.getInstance().buildProxy(tmpBeanDef.getBeanClass());
			cacheProxyBean.put(beanName, bean);
			return bean;
		}
		return null;
	}
	
	
	@SuppressWarnings("unchecked")
	public <T> T getBean(String name, Class<T> requiredType) {
		Object obj = lazyBeanFactory.getBean(name, requiredType);
		if(obj == null) {
			throw new NoSuchBeanDefinitionException(name);
		}
		return (T) obj;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		if(org.springframework.core.env.Environment.class.isAssignableFrom(requiredType)) {
			return (T) getEnvironment();
		}else if(org.springframework.context.ApplicationContext.class.isAssignableFrom(requiredType)) {
			return (T) this;
		}
		return (T) LazyBean.getInstance().buildProxy(requiredType);
	}
	
	public Object getProxyBeanByClass(Class<?> tagClass) {
		if(org.springframework.core.env.Environment.class.isAssignableFrom(tagClass)) {
			return getEnvironment();
		}else if(org.springframework.context.ApplicationContext.class.isAssignableFrom(tagClass)) {
			return this;
		}else if(org.springframework.beans.factory.BeanFactory.class.isAssignableFrom(tagClass)) {
			return lazyBeanFactory;
		}
		String beanName = BeanNameUtil.getBeanName(tagClass);
		return cacheProxyBean.get(beanName);
	}

	public Object getProxyBeanByClassAndBeanName(String beanName, Class<?> tagClass) {
		Object proxyBean = cacheProxyBean.get(beanName);
		if(tagClass.isInstance(proxyBean)) {//如果不是，则有可能重名
			return proxyBean;
		}
		return getProxyBeanByClass(tagClass);
	}
	
	private Map<Class<?>,Set<String>> classMapName = Maps.newHashMap();
	
	/**
	 *注册代理对象
	 */
	public void registProxyBean(String beanName, Object proxyBean, Class<?> tagC) {
		if(proxyBean instanceof String) {
			log.error("{}异常处理",beanName);
			return;
		}
		if(StringUtils.isBlank(beanName)) {
			throw new JunitException("注册bean，beanName不能为空",true);
		}
		if(cacheProxyBean.containsKey(beanName)) {
			Object existsBean = cacheProxyBean.get(beanName);
			if(tagC.isInstance(existsBean)) {//如果不是，则有可能重名
				log.warn("{}=>已存在",beanName);
				return ;
			}else {
				beanName = beanName+"#"+proxyBean.hashCode();
				log.warn("新注册代理bean:{}",beanName);
				registProxyBean(beanName, proxyBean, tagC);
			}
		}
		
		if(!LazyProxyManager.isProxy(proxyBean)) {
			log.warn("beanName：{},tagC:{}",beanName,tagC);
			throw new JunitException("注册bean非代理bean，不能在这里处理",true);
		}
		
		cacheProxyBean.put(beanName, proxyBean);
		if(!classMapName.containsKey(tagC)) {
			classMapName.put(tagC, Sets.newHashSet());
		}
		classMapName.get(tagC).add(beanName);
	}
	
	public void releaseBean(Object tmp, Class<?> tagC) {
		lazyBeanFactory.releaseBean(tagC, tmp);
	}
	
	private Properties properties;
	private ConfigurableEnvironment env;
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if(env == null) {
			buildEnv();
		}
		return env;
	}

	private String StandardServletEnvironment = "org.springframework.web.context.support.StandardServletEnvironment";
	private synchronized void buildEnv() {
		if(env == null) {
			Class<?> StandardServletEnvironmentC = JunitClassLoader.getInstance().junitloadClass(StandardServletEnvironment);
			if(StandardServletEnvironmentC!=null) {
				try {
					env = (ConfigurableEnvironment) StandardServletEnvironmentC.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new JunitException(e, true);
				}
			}else {
				env = new StandardEnvironment();
			}
			new EventPublishingRunListener(new SpringApplication(),new String[] {})
				.environmentPrepared(new DefaultBootstrapContext(), env);
			getBeanFactory().registerSingleton("environment", env);
		}
	}
	
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {
		return LazyBean.findBeanWithAnnotation(annotationType);
	}
	
//	@Override
//	public void register() {
//		ContainerManager.registComponent(this);
//	}
//	
//	@Override
//	public String getBeanKey() {
//		return JunitApplicationContext.class.getSimpleName();
//	}
}
