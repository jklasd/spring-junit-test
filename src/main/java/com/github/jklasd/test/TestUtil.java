package com.github.jklasd.test;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.StandardServletEnvironment;

import com.github.jklasd.test.spring.JavaBeanUtil;
import com.github.jklasd.test.spring.TestApplicationContext;
import com.github.jklasd.test.spring.XmlBeanUtil;
import com.github.jklasd.util.LogbackUtil;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jubin.zhang
 *	2020-11-19
 * 工具入口类
 */
@Slf4j
public class TestUtil{
	private static Set<String> scanClassPath = Sets.newHashSet();
	public static void loadScanPath(String... scanPath) {
		for(String path : scanPath) {
			scanClassPath.add(path);
		}
	}
	private TestUtil() {
		log.info("--实例化TestUtil--");
	}
	private static ApplicationContext applicationContext;
	
	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = new TestApplicationContext(applicationContext);
	}
	/**
	 * 处理配置
	 * 如：XML配置，java代码 Bean配置
	 * 静态工具类bean处理
	 */
	private void processConfig() {
		XmlBeanUtil.process();
		JavaBeanUtil.process();
		
		List<Class<?>> list = ScanUtil.findStaticMethodClass();
		log.debug("static class =>{}",list.size());
		/**
		 * 不能是抽象类
		 */
		list.stream().filter(classItem -> classItem != getClass() && !Modifier.isAbstract(classItem.getModifiers())).forEach(classItem->{
			log.debug("static class =>{}",classItem);
			LazyBean.processStatic(classItem);
		});
	}
	
	
	public static Object getExistBean(Class<?> classD) {
		if(classD == ApplicationContext.class) {
			return getApplicationContext();
		}
		Object obj = getApplicationContext().getBean(classD);
		return obj;
	}
	
	@SuppressWarnings("unchecked")
	public static Object buildBean( Class c) {
		Object obj = null;
		try {
			obj = getApplicationContext().getBean(c);
			if(obj!=null) {
				return obj;
			}
		} catch (Exception e) {
			log.error("不存在");
		}
		obj = getApplicationContext().getAutowireCapableBeanFactory().createBean(c);
		return obj; 
	}
	
	public static void registerBean(Object bean) {
		DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) getApplicationContext().getAutowireCapableBeanFactory();
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
	
	/**
	 * 获取存在Service,Complent的相关对象
	 * @param classD bean 类型
	 * @param beanName 名称
	 * @return 返回容器中已存在的Bean 
	 */
	@SuppressWarnings("unchecked")
	public static Object getExistBean(Class classD,String beanName) {
		try {
			if(classD == ApplicationContext.class
					|| ScanUtil.isExtends(ApplicationContext.class, classD)) {
				return getApplicationContext();
			}else if(classD == Environment.class) {
				return getApplicationContext().getEnvironment();
			}
			Object obj = getApplicationContext().getBean(classD);
			return obj;
		}catch(NullPointerException e) {
			return null;
		}catch (NoUniqueBeanDefinitionException e) {
			if(beanName != null) {
				Object obj = getApplicationContext().getBean(beanName);
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
		key = key.replace("${", "").replace("}", "");
		if(getApplicationContext()!=null) {
			String[] keys = key.split(":");
			String value = getApplicationContext().getEnvironment().getProperty(keys[0]);
			if(value!=null) {
				return value;
			}else {
				return keys.length>1?keys[1]:(defaultStr == null?key:defaultStr);
			}
		}
		return key;
	}
	public static String getPropertiesValue(String key) {
		return getPropertiesValue(key,null);
	}
	public static Object value(String key,Class<?> type) {
//		if(key.contains("finalPV.userId")) {
//			log.debug("断点");
//		}
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
			}else if(type != String.class) {
				return null;
			}
		} catch (Exception e) {
			log.warn("转换类型异常{}==>{}",key,type);
			throw e;
		}
		
		return value;
	}
	
	public static PropertySources getPropertySource() {
		StandardEnvironment env = (StandardEnvironment) getApplicationContext().getEnvironment();
		return env.getPropertySources();
	}
	/**
	 * 启动方法
	 * @param obj 执行目标对象
	 */
	public static void startTestForNoContainer(Object obj) {
		LazyBean.processAttr(obj, obj.getClass());
		TestUtil launch = new TestUtil();
		launch.setApplicationContext(null);
		Resource logback = applicationContext.getResource("logback.xml");
		if(logback!=null && logback.exists()) {
			LogbackUtil.init(logback,(StandardServletEnvironment) getApplicationContext().getEnvironment());
			log.info("加载环境配置完毕");
		}
		ScanUtil.loadAllClass();
		launch.processConfig();
	}

	public static Boolean isScanClassPath(String cn) {
//		if(cn.contains("RedisAutoConfiguration")) {
//		boolean  judgment = scanClassPath.stream().anyMatch(p -> cn.contains(p));
//			log.info("断点");
//		}
		return scanClassPath.stream().anyMatch(p -> cn.contains(p));
	}
//	public static void getExistBean(Class interfaceClass, Type[] classGeneric) {
//		DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) getApplicationContext().getAutowireCapableBeanFactory();
//		DependencyDescriptor dd = new DependencyDescriptor(null, false);
//		dlbf.resolveDependency(dd, interfaceClass.getName());
//	}
}
