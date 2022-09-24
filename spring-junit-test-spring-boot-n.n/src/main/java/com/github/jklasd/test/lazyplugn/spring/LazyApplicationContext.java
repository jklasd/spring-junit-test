package com.github.jklasd.test.lazyplugn.spring;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.abstrac.JunitApplicationContext;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.exception.JunitException;
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
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type){
		return lazyBeanFactory.getBeansOfType(type);
	}
	
	Map<String,Object> cacheProxyBean = Maps.newHashMap();
	
	public Object getBean(String beanName) {
		return lazyBeanFactory.getBean(beanName);
	}
	
	
	public <T> T getBean(String name, Class<T> requiredType) {
		return lazyBeanFactory.getBean(name, requiredType);
	}
	
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return lazyBeanFactory.getBean(requiredType);
	}
	
	public Object getProxyBeanByClass(Class<?> tagClass) {
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
	 * bean 注册
	 * @param beanName
	 * @param tmp
	 * @param tagC
	 */
	public void registProxyBean(String beanName, Object proxyBean, Class<?> tagC) {
		if(StringUtils.isBlank(beanName)) {
			throw new JunitException("注册bean，beanName不能为空",true);
		}
		if(cacheProxyBean.containsKey(beanName)) {
			log.warn("{}=>已存在",beanName);
			return;
		}
		
		if(!LazyProxyManager.isProxy(proxyBean)) {
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

	private synchronized void buildEnv() {
		if(env == null) {
			env = new StandardEnvironment();
			if(properties == null) {
				properties = new Properties();
				try {
					Resource propRes = ScanUtil.getRecourceAnyOne("application.properties","config/application.properties");
					if(propRes!=null && propRes.exists()) {
						properties.load(propRes.getInputStream());
						String active = null;
						if(StringUtils.isNotBlank(active = properties.getProperty("spring.profiles.active"))) {
							Resource activePropRes = ScanUtil.getRecourceAnyOne("application-"+active+".properties","config/application-"+active+".properties");
							if(activePropRes!=null && activePropRes.exists()) {
								properties.load(activePropRes.getInputStream());
							}
						}
					}else {
//						Object yml = Class.forName("Ymal").newInstance();
						Resource ymlRes = ScanUtil.getRecourceAnyOne("application.yml","config/application.yml");
						if(ymlRes!=null && ymlRes.exists()) {
							YamlPropertiesFactoryBean ymlToProp = new YamlPropertiesFactoryBean();
							ymlToProp.setResources(ymlRes);
							properties.putAll(ymlToProp.getObject());
							String active = null;
							if(StringUtils.isNotBlank(active = properties.getProperty("spring.profiles.active"))) {
								Resource activeYmlRes = ScanUtil.getRecourceAnyOne("application-"+active+".yml","config/application-"+active+".yml");
								if(activeYmlRes!=null && activeYmlRes.exists()) {
									ymlToProp.setResources(activeYmlRes);
									properties.putAll(ymlToProp.getObject());
								}
							}
							
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			env.getPropertySources().addFirst(new PropertiesPropertySource("junitProp", properties));
		}
	}
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {
		return LazyBean.findBeanWithAnnotation(annotationType);
	}

	@Override
	public void register() {
		ContainerManager.registComponent(this);
	}
	
	@Override
	public String getBeanKey() {
		return JunitApplicationContext.class.getSimpleName();
	}
}
