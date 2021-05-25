package com.github.jklasd.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;

import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.JavaBeanUtil;
import com.github.jklasd.test.lazyplugn.spring.TestApplicationContext;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.github.jklasd.test.util.LogbackUtil;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jubin.zhang 2020-11-19 工具入口类
 */
@Slf4j
public class TestUtil{
	private Set<String> scanClassPath = Sets.newHashSet();
	private Set<String> scanPropertiesList = Sets.newHashSet();

	public void loadProperties(String... scanPropertiesPath) {
		for (String path : scanPropertiesPath) {
			scanPropertiesList.add(path);
		}
	}

	public void loadScanPath(String... scanPath) {
		for (String path : scanPath) {
			scanClassPath.add(path);
		}
	}

	private TestUtil() {
	    try {
            Resource banner = ScanUtil.getRecourceAnyOne("testutil.txt");
            if(banner!=null) {
                BufferedReader bis = new BufferedReader(new InputStreamReader(banner.getInputStream()));
                String line = null;
                while((line = bis.readLine())!=null) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
             e.printStackTrace();
        }
	}
	private static volatile TestUtil bean;
	public synchronized static TestUtil getInstance() {
	    if(bean!=null) {
	        return bean;
	    }
	    bean = new TestUtil();
	    bean.setApplicationContext(null);
	    return bean;
	}
	
    private TestApplicationContext applicationContext;

	public TestApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = new TestApplicationContext(applicationContext);
	}

	/**
	 * 处理配置 如：XML配置，java代码 Bean配置 静态工具类bean处理
	 */
	private volatile boolean processed;
	private void processConfig() {
	    if(processed) {
            return;
        }
	    processed = true;
	    log.info("====加载配置====");
//		LazyMybatisMapperBean.getInstance().configure();
		
		XmlBeanUtil.getInstance().process();
		JavaBeanUtil.process();

		List<Class<?>> list = ScanUtil.findStaticMethodClass();
		log.debug("static class =>{}", list.size());
		/**
		 * 不能是抽象类
		 */
		list.stream().filter(classItem -> classItem != getClass() && !Modifier.isAbstract(classItem.getModifiers()))
				.forEach(classItem -> {
					log.debug("static class =>{}", classItem);
//					if(classItem.getName().contains("HandleMessageUtil")) {
//					    System.out.println("断点");
//					}
					LazyBean.getInstance().processStatic(classItem);
				});
	}

	public Object getExistBean(Class<?> classD) {
		if (classD == ApplicationContext.class) {
			return getApplicationContext();
		}
		Object obj = getApplicationContext().getBean(classD);
		return obj;
	}

	/**
	 * 获取存在Service,Complent的相关对象
	 * 
	 * @param classD   bean 类型
	 * @param beanName 名称
	 * @return 返回容器中已存在的Bean
	 */
	@SuppressWarnings("unchecked")
	public Object getExistBean(Class classD, String beanName) {
		try {
			if (classD == ApplicationContext.class || ScanUtil.isExtends(ApplicationContext.class, classD)) {
				return getApplicationContext();
			} else if (classD == Environment.class) {
				return getApplicationContext().getEnvironment();
			}
			Object obj = getApplicationContext().getBean(classD);
			return obj;
		} catch (NullPointerException e) {
			return null;
		} catch (NoUniqueBeanDefinitionException e) {
			if (beanName != null) {
				Object obj = getApplicationContext().getBean(beanName);
				return obj;
			}
			return null;
		} catch (NoSuchBeanDefinitionException e) {
			return null;
		} catch (UnsatisfiedDependencyException e) {
			log.error("UnsatisfiedDependencyException=>{},{}获取异常", classD, beanName);
			return null;
		} catch (BeanCreationException e) {
			log.error("BeanCreationException=>{},{}获取异常", classD, beanName);
			return null;
		}
	}

	public String getPropertiesValue(String key, String defaultStr) {
		key = key.replace("${", "").replace("}", "");
		if (getApplicationContext() != null) {
			String[] keys = key.split(":");
			String value = getApplicationContext().getEnvironment().getProperty(keys[0]);
			if (value != null) {
				return value;
			} else {
				return keys.length > 1 ? keys[1] : (defaultStr == null ? key : defaultStr);
			}
		}
		return key;
	}

	public String getPropertiesValue(String key) {
		return getPropertiesValue(key, null);
	}

	public Object value(String key, Class<?> type) {
//		if(key.contains("finalPV.userId")) {
//			log.debug("断点");
//		}
		String value = getPropertiesValue(key);
		try {
			if (StringUtils.isNotBlank(value)) {
				if (type == null || type == String.class) {
					return value;
				} else if (type == Integer.class || type == int.class) {
					return Integer.valueOf(value);
				} else if (type == Long.class || type == long.class) {
					return Long.valueOf(value);
				} else if (type == Double.class || type == double.class) {
					return Double.valueOf(value);
				} else if (type == BigDecimal.class) {
					return new BigDecimal(value);
				} else if (type == Boolean.class || type == boolean.class) {
					return new Boolean(value);
				} else if (type == Class.class) {
                    return ScanUtil.loadClass(value);
                }{
					log.info("TestUil 类型转换=========其他类型========={}=",type);
				}
			} else if (type != String.class) {
				return null;
			}
		} catch (Exception e) {
			log.warn("转换类型异常{}==>{}", key, type);
			throw e;
		}

		return value;
	}

	public PropertySources getPropertySource() {
		StandardEnvironment env = (StandardEnvironment) getApplicationContext().getEnvironment();
		return env.getPropertySources();
	}

	/**
	 * 启动方法
	 * 
	 * @param obj 执行目标对象
	 */
	private static volatile boolean processInited;
	public static void startTestForNoContainer(Object obj) {
	    if(processInited) {
	        return;
	    }
		TestUtil launch = getInstance();
		launch.loadProp();
		LazyBean.getInstance().processAttr(obj, obj.getClass());
		LogbackUtil.resetLog();
		ScanUtil.loadAllClass();
		launch.processConfig();
	}

	private void loadProp() {
		ConfigurableEnvironment cEnv = (ConfigurableEnvironment) getApplicationContext().getEnvironment();
		Properties properties = new Properties();
		for(String propPath : scanPropertiesList) {
			try {
				Resource propRes = ScanUtil.getRecourceAnyOne(propPath);
				if (propRes != null && propRes.exists()) {
					properties.load(propRes.getInputStream());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		cEnv.getPropertySources().addLast(new PropertiesPropertySource("loadProp", properties));
	}

	public Boolean isScanClassPath(String cn) {
		return scanClassPath.stream().anyMatch(p -> cn.contains(p));
	}

}
