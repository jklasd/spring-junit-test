package com.github.jklasd.test.lazyplugn.spring;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;

import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.util.ScanUtil;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyApplicationContext extends GenericApplicationContext{
	
	@Getter
	private DefaultListableBeanFactory lazyBeanFactory;
	private static LazyApplicationContext lazyApplicationContext;
	public static LazyApplicationContext getInstance() {
		if(lazyApplicationContext == null) {
			lazyApplicationContext = new LazyApplicationContext();
		}
		return lazyApplicationContext;
	}
	private LazyApplicationContext() {
		super(LazyListableBeanFactory.getInstance());
		lazyBeanFactory = getDefaultListableBeanFactory();
	}
	
	public Object getBean(String beanName) {
		try {
			if(lazyBeanFactory.containsBean(beanName)) {
				return lazyBeanFactory.getBean(beanName);
			}
			/**
			 * 尝试筛选域中，查看bean是否存在
			 */
			Class<?> beanClass = ScanUtil.findClassByName(beanName);
			if(beanClass!=null) {
				return LazyBean.getInstance().buildProxy(beanClass,beanName);
			}
			return null;
		}catch(Exception e){
			throw new JunitException("Bean 未找到");
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getBean(String name, Class<T> requiredType) {
		try {
			return lazyBeanFactory.getBean(name,requiredType);
		} catch (NoSuchBeanDefinitionException e) {
			if(requiredType == EntityScanPackages.class) {
				throw e;
			}
			return (T) LazyBean.getInstance().buildProxy(requiredType, name);
		}
	}
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		try {
			return lazyBeanFactory.getBean(requiredType);
		} catch (NoSuchBeanDefinitionException e) {
			return (T) LazyBean.getInstance().buildProxy(requiredType);
		}
	}
	
	public Object getBeanByClass(Class<?> factoryclass) {
		try {
			return lazyBeanFactory.getBean(factoryclass);
		} catch (BeansException e) {
		}
		return null;
	}

	public Object getBeanByClassAndBeanName(String beanName, Class<?> tagClass) {
		try {
			return lazyBeanFactory.getBean(beanName, tagClass);
		}catch(JunitException e){
			if(e.isNeed_throw()) {
				throw e;
			}
			return null;
		}catch(Exception e) {
			log.debug("getBeanByClassAndBeanName,beanName=>{},tagClass=>{}",beanName,tagClass);
			return null;
		}
	}
	
	public void registBean(String beanName, Object tmp, Class<?> tagC) {
//		regist
		if(beanName!=null) {
			if(!lazyBeanFactory.containsBean(beanName)) {
				lazyBeanFactory.registerSingleton(beanName, tmp);
			}else {
				log.debug("bean已存在");
			}
		}else {
			lazyBeanFactory.registerResolvableDependency(tagC, tmp);
		}
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
						YamlPropertiesFactoryBean ymlToProp = new YamlPropertiesFactoryBean();
//						Object yml = Class.forName("Ymal").newInstance();
						Resource ymlRes = ScanUtil.getRecourceAnyOne("application.yml","config/application.yml");
						if(ymlRes!=null && ymlRes.exists()) {
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
}
