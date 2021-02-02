package com.junit.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.logging.logback.LogbackUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.StandardServletEnvironment;

import com.google.common.collect.Maps;
import com.junit.test.spring.TestApplicationContext;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jubin.zhang
 *	2020-11-19
 *
 */
@Slf4j
@Component
public class TestUtil implements ApplicationContextAware,BeanPostProcessor{
	private static boolean test;
	public static String mapperPath = "classpath*:/mapper/**/*.xml";
	public static String mapperScanPath = "com.mapper";
	public TestUtil() {
		log.info("实例化TestUtil");
	}
	public static PooledDataSource dataSource;
	private static Map<String,Object> lazyBeanObjMap;
	private static Map<String,Field> lazyBeanFieldMap;
	private static Map<String,String> lazyBeanNameMap;
	
	static void loadLazyAttr(Object obj,Field f,String beanName) {
		if(lazyBeanObjMap == null) {
			lazyBeanObjMap = Maps.newHashMap();
			lazyBeanFieldMap = Maps.newHashMap();
			lazyBeanNameMap = Maps.newHashMap();
		}
		String fKey = obj.getClass().getName()+"_"+f.getName();
		lazyBeanObjMap.put(fKey,obj);
		lazyBeanFieldMap.put(fKey,f);
		lazyBeanNameMap.put(fKey, beanName);
	}
	
	private static ApplicationContext applicationContext;
	
	public static void buildDataSource(String url,String username,String passwd) {
		if(dataSource == null) {
			dataSource = new PooledDataSource();
		}
		dataSource.setUrl(TestUtil.getPropertiesValue("jdbc.url"));
		dataSource.setUsername(TestUtil.getPropertiesValue("jdbc.username"));
		dataSource.setPassword(TestUtil.getPropertiesValue("jdbc.password"));
		dataSource.setDriver(TestUtil.getPropertiesValue("jdbc.driver",""));
	}
	
	public static boolean isTest() {
		return test;
	}
	public static void openTest() {
		test = true;
	}
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = new TestApplicationContext(applicationContext);
	}
	private void lazyProcessAttr() {
		if(lazyBeanObjMap!=null) {
			lazyBeanObjMap.keySet().forEach(fKey->{
				Object obj = lazyBeanObjMap.get(fKey);
				Field attr = lazyBeanFieldMap.get(fKey);
				try {
					attr.set(obj, LazyBean.buildProxy(attr.getType(),lazyBeanNameMap.get(fKey)));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			});
		}
	}
	private void processConfig() {
		
		List<Class> list = ScanUtil.findStaticMethodClass();
		log.info("static class =>{}",list.size());
//		String key = "redis.node1.port";
//		log.info("{}=>{}",key,getPropertiesValue(key));
		/**
		 * 不能是抽象类
		 */
		list.stream().filter(classItem -> classItem != getClass() && !Modifier.isAbstract(classItem.getModifiers())).forEach(classItem->{
			log.info("static class =>{}",classItem);
			LazyBean.processStatic(classItem);
		});
	}
	
	public static Object getExistBean(Class<?> classD) {
		if(classD == ApplicationContext.class) {
			return getStaticApplicationContext();
		}
		Object obj = getStaticApplicationContext().getBean(classD);
		return obj;
	}
	
	@SuppressWarnings("unchecked")
	public static Object buildBean( Class c) {
		Object obj = null;
		try {
			obj = getStaticApplicationContext().getBean(c);
			if(obj!=null) {
				return obj;
			}
		} catch (Exception e) {
			log.error("不存在");
		}
		obj = getStaticApplicationContext().getAutowireCapableBeanFactory().createBean(c);
		return obj; 
	}
	
	public static void registerBean(Object bean) {
		DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) getStaticApplicationContext().getAutowireCapableBeanFactory();
		Object obj = null;
		try {
			obj = dlbf.getBean(bean.getClass());
		} catch (Exception e) {
			log.error("不存在");
		}
		if(obj==null) {
			dlbf.registerSingleton(bean.getClass().getPackage().getName()+"."+bean.getClass().getSimpleName(), bean);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Object getExistBean(Class classD,String beanName) {
		try {
			if(classD == ApplicationContext.class) {
				return getStaticApplicationContext();
			}
			Object obj = getStaticApplicationContext().getBean(classD);
			return obj;
		}catch(NullPointerException e) {
			return null;
		}catch (NoUniqueBeanDefinitionException e) {
			if(beanName != null) {
				Object obj = getStaticApplicationContext().getBean(beanName);
				return obj;
			}
			return null;
		}catch (NoSuchBeanDefinitionException e) {
			return null;
		}catch(UnsatisfiedDependencyException e) {
			log.error("UnsatisfiedDependencyException=>{},{}获取异常",classD,beanName);
			return null;
		}catch (BeanCreationException e) {
			log.error("BeanCreationException=>{},{}获取异常",classD,beanName);
			return null;
		}
	}
	public static String getPropertiesValue(String key,String defaultStr) {
		if(getStaticApplicationContext()!=null) {
			String[] keys = key.split(":");
			String value = getStaticApplicationContext().getEnvironment().getProperty(keys[0]);
			if(StringUtils.isNotBlank(value)) {
				return value;
			}else {
				return keys.length>1?keys[1]:defaultStr;
			}
		}
		return "";
	}
	public static String getPropertiesValue(String key) {
		return getPropertiesValue(key,null);
	}
	public static Object value(Object obj, String key,Class type) {
		String value = getPropertiesValue(key);
		try {
			if(StringUtils.isNotBlank(value)) {
				if(type == null || type == String.class) {
					return	value;
				}else if(type == Integer.class || type == int.class) {
					return Integer.valueOf(value);
				}else if(type == Long.class || type == long.class) {
					return Long.valueOf(value);
				}else if(type == Double.class || type == double.class) {
					return Double.valueOf(value);
				}else if(type == BigDecimal.class) {
					return new BigDecimal(value);
				}else if(type == Boolean.class || type == boolean.class) {
					return new Boolean(value);
				}else {
					log.info("其他类型");
				}
			}
		} catch (Exception e) {
			System.out.println(obj.getClass().getName()+"转换类型异常"+key+"==>"+type);
			log.error("转换类型异常",e);
		}
		
		return null;
	}
	
	public static String dubboXml = "classpath*:/dubbo-context.xml";
	public static String mapperJdbcPrefix = "";
	
	public static PropertySources getPropertySource() {
		StandardEnvironment env = (StandardEnvironment) getStaticApplicationContext().getEnvironment();
		return env.getPropertySources();
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		// TODO Auto-generated method stub
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		// TODO Auto-generated method stub
		return bean;
	}

	public static void startTestForNoContainer(Object obj) {
		LazyBean.processAttr(obj, obj.getClass());
		TestUtil launch = new TestUtil();
		launch.setApplicationContext(null);
		Resource logback = applicationContext.getResource("logback.xml");
		if(logback != null) {
			LogbackUtil.init((StandardServletEnvironment) getStaticApplicationContext().getEnvironment());
			log.info("加载完毕");
		}
		ScanUtil.loadAllClass();
		launch.processConfig();
		launch.lazyProcessAttr();
		
	}

	public static ApplicationContext getStaticApplicationContext() {
		return applicationContext;
	}

	public void setStaticApplicationContext(ApplicationContext staticApplicationContext) {
		this.applicationContext = staticApplicationContext;
	}
}
