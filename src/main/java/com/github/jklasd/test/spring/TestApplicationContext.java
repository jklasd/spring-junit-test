package com.github.jklasd.test.spring;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.domain.EntityScanPackagesConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.StandardServletEnvironment;

import com.github.jklasd.test.LazyBean;
import com.github.jklasd.test.ScanUtil;

public class TestApplicationContext implements ConfigurableApplicationContext{

	private ApplicationContext parentContext;
	public TestApplicationContext(ApplicationContext context) {
		this.parentContext = context;
	}
	
	private Properties properties;
	private StandardServletEnvironment env;
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if(parentContext == null || parentContext == this) {
			if(env == null) {
				env = new StandardServletEnvironment();
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
//							Object yml = Class.forName("Ymal").newInstance();
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
		return (ConfigurableEnvironment) parentContext.getEnvironment();
	}

	@Override
	public boolean containsBeanDefinition(String beanName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getBeanDefinitionCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String[] getBeanDefinitionNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getBeanNamesForType(ResolvableType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {
		return LazyBean.findBeanWithAnnotation(annotationType);
	}

	@Override
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {
		return null;
	}

	@Override
	public Object getBean(String name) throws BeansException {
		if(parentContext == null || parentContext == this) {
			if(beanDefinitionMap.containsKey(name)) {
				return beanDefinitionMap.get(name);
			}
			if(getAutowireCapableBeanFactory().containsBean(name)) {
				return getAutowireCapableBeanFactory().getBean(name);
			}
			return LazyBean.findBean(name);
		}
		if(parentContext.containsBean(name)) {
			return parentContext.getBean(name);
		}else {
			return LazyBean.findBean(name);
		}
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		if(parentContext == null || parentContext == this) {
			if(requiredType.getName().contains("EntityScanPackages")) {
				return (T) EntityScanPackagesConstructor.getBean();
			}
			return (T)LazyBean.findBean(name, requiredType);
		}
		try {
			Object bean = parentContext.getBean(name,requiredType);
			return (T) bean;
		} catch (NoSuchBeanDefinitionException e) {
			return (T)LazyBean.findBean(name, requiredType);
		}
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		if(parentContext == null || parentContext == this) {
			if(beanForClassMap.containsKey(requiredType)) {
				return (T) beanForClassMap.get(requiredType);
			}
			if(getAutowireCapableBeanFactory().getBean(requiredType) != null) {
				return getAutowireCapableBeanFactory().getBean(requiredType);
			}
			return (T)LazyBean.findBean(requiredType);
		}
		try {
			Object bean = parentContext.getBean(requiredType);
			return (T) bean;
		} catch (NoSuchBeanDefinitionException e) {
			return (T)LazyBean.findBean(requiredType);
		}
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		if(parentContext == null || parentContext == this) {
			return null;
		}
		return parentContext.getBean(name, args);
	}

	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean containsBean(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getAliases(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.springframework.beans.factory.BeanFactory getParentBeanFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean containsLocalBean(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void publishEvent(ApplicationEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void publishEvent(Object event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return ScanUtil.getResources(locationPattern);
	}

	@Override
	public Resource getResource(String location) {
		try {
			return ScanUtil.getRecource(location);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public ClassLoader getClassLoader() {
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getApplicationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public long getStartupDate() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ApplicationContext getParent() {
		return parentContext;
	}

	private DefaultListableBeanFactory beanFactory;
	
	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	public Properties getProperties() {
		return properties;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setId(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setParent(ApplicationContext parent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addProtocolResolver(ProtocolResolver resolver) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void refresh() throws BeansException, IllegalStateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerShutdownHook() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException {
		if(beanFactory == null) {
			beanFactory = new DefaultListableBeanFactory(parentContext!=null?parentContext:this);
		}
		return beanFactory;
	}
	
	private final Map<String, Object> beanDefinitionMap = new ConcurrentHashMap<String, Object>(256);
	private final Map<Class<?>, Object> beanForClassMap = new ConcurrentHashMap<>(256);
	public void registBean(String beanName, Object newBean ,Class beanClass) {
		if(newBean!=null) {
			if(StringUtils.isNotBlank(beanName)) {
				beanDefinitionMap.put(beanName, newBean);
			}
			if(!beanForClassMap.containsKey(beanClass)) {
				beanForClassMap.put(beanClass, newBean);
			}
		}
	}

}
