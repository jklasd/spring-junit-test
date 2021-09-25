package com.github.jklasd.test.lazyplugn.spring;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;

import com.github.jklasd.test.util.ScanUtil;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyApplicationContext extends GenericApplicationContext{
	
	@Getter
	private DefaultListableBeanFactory lazyBeanFactory;
	public LazyApplicationContext() {
		super(new LazyListableBeanFactory());
		lazyBeanFactory = getDefaultListableBeanFactory();
	}
	
	
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return lazyBeanFactory.getBean(requiredType);
	}
	
	public Object getBeanByClass(Class<?> factoryclass) {
		try {
			return getBean(factoryclass);
		} catch (BeansException e) {
		}
		return null;
	}

	public Object getBeanByClassAndBeanName(String beanName, Class<?> tagClass) {
		try {
			return lazyBeanFactory.getBean(beanName, tagClass);
		}catch(Exception e) {
			log.error("getBeanByClassAndBeanName,beanName=>{},tagClass=>{}",beanName,tagClass);
			log.error("getBeanByClassAndBeanName",e);
			return null;
		}
	}

	public void registBean(String beanName, Object tmp, Class<?> tagC) {
//		regist
		if(beanName!=null) {
			lazyBeanFactory.registerSingleton(beanName, tmp);
		}else {
			lazyBeanFactory.registerResolvableDependency(tagC, tmp);
		}
	}
	
	private Properties properties;
	private ConfigurableEnvironment env;
	@Override
	public ConfigurableEnvironment getEnvironment() {
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
			env.getPropertySources().addFirst(new PropertiesPropertySource("local", properties));
		}
		return env;
	}

}
