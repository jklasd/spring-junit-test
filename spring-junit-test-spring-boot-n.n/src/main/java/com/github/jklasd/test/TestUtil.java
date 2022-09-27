package com.github.jklasd.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.abstrac.JunitApplicationContext;
import com.github.jklasd.test.common.component.ScannerRegistrarComponent;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.interf.register.JunitCoreComponentI;
import com.github.jklasd.test.common.util.LogbackUtil;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.core.facade.ResourceLoader;
import com.github.jklasd.test.core.facade.processor.BeanFactoryProcessor;
import com.github.jklasd.test.core.facade.scan.BeanCreaterScan;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.core.facade.scan.PropResourceManager;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jubin.zhang 2020-11-19 工具入口类
 */
@Slf4j
public class TestUtil implements JunitCoreComponentI{
	@Getter
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

	private TestUtil() {}
	private static volatile TestUtil bean;
	public synchronized static TestUtil getInstance() {
	    if(bean!=null) {
	        return bean;
	    }
	    bean = new TestUtil();
	    bean.register();
//	    bean.setApplicationContext(null);
	    bean.applicationContext = LazyApplicationContext.getInstance();
	    bean.applicationContext.refresh();
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
	    return bean;
	}
	
    private LazyApplicationContext applicationContext;

	public JunitApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
	public ConfigurableEnvironment getEnvironment() {
		return applicationContext.getEnvironment();
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
	    ContainerManager.stats = ContainerManager.init;
	    log.debug("=========加载配置========");
	    ResourceLoader.getInstance().init();
		ScannerRegistrarComponent.process();
		BeanFactoryProcessor.getInstance().postProcessBeanFactory(getApplicationContext().getBeanFactory());
		ContainerManager.stats = ContainerManager.inited;
		JunitClassLoader.getInstance().processStatic();
	}

	public Object getExistBean(Class<?> classD) {
		if (classD == ApplicationContext.class) {
			return getApplicationContext();
		}
		Object obj = getApplicationContext().getBean(classD);
		return obj;
	}

//	/**
//	 * 获取存在Service,Complent的相关对象
//	 * 
//	 * @param classD   bean 类型
//	 * @param beanName 名称
//	 * @return 返回容器中已存在的Bean
//	 */
//	@SuppressWarnings("unchecked")
//	public Object getExistBean(Class classD, String beanName) {
//		try {
//			if (classD == ApplicationContext.class || ScanUtil.isExtends(ApplicationContext.class, classD)) {
//				return getApplicationContext();
//			} else if (classD == Environment.class) {
//				return getApplicationContext().getEnvironment();
//			}
//			if (beanName != null) {
//				Object obj = getApplicationContext().getBean(beanName);
//				return obj;
//			}
//			Object obj = getApplicationContext().getBean(classD);
//			return obj;
//		} catch (NullPointerException e) {
//			return null;
//		} catch (NoUniqueBeanDefinitionException e) {
//			if (beanName != null) {
//				Object obj = getApplicationContext().getBean(beanName);
//				return obj;
//			}
//			return null;
//		} catch (NoSuchBeanDefinitionException e) {
//			return null;
//		} catch (UnsatisfiedDependencyException e) {
//			log.error("UnsatisfiedDependencyException=>{},{}获取异常", classD, beanName);
//			return null;
//		} catch (BeanCreationException e) {
//			log.error("BeanCreationException=>{},{}获取异常", classD, beanName);
//			return null;
//		}
//	}

//	public String getPropertiesValue(String key, String defaultStr) {
//		key = key.replace("${", "").replace("}", "");
//		if (getApplicationContext() != null) {
//			String[] keys = key.split(":");
//			String value = getApplicationContext().getEnvironment().getProperty(keys[0]);
//			if (value != null) {
//				return value;
//			} else {
//				if(keys.length > 1) {
//					return keys[1];
//				}else {
//					if(key.contains(":")) {
//						return "";
//					}else {
//						return defaultStr == null ? key : defaultStr;
//					}
//				}
//			}
//		}
//		return key;
//	}

//	public String getPropertiesValue(String key) {
//		return getPropertiesValue(key, null);
//	}

	public Object valueFromEnvForAnnotation(String key, Class<?> type) {
		String value = getApplicationContext().getEnvironment().resolvePlaceholders(key);
		try {
			if (StringUtils.isNotBlank(value)) {
				if (type == null || type == String.class) {
					return value;
				}else if (type == Byte.class || type == byte.class) {
					return Byte.valueOf(value);
				}else if (type == Integer.class || type == int.class) {
					return Integer.valueOf(value);
				} else if (type == Long.class || type == long.class) {
					return Long.valueOf(value);
				} else if (type == Double.class || type == double.class) {
					return Double.valueOf(value);
				} else if (type == BigDecimal.class) {
					return new BigDecimal(value);
				} else if (type == Boolean.class || type == boolean.class) {
					return new Boolean(value);
				} else if (type == char.class) {
					return value.charAt(0);
				} else if (type == Class.class) {
                    return ScanUtil.loadClass(value);
                }else{
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
	
	@Deprecated
	public static void startTestForNoContainer(Object obj) {
		resourcePreparation();
		//注入当前执行对象
		LazyBean.getInstance().processAttr(obj, obj.getClass());
//		handlerFirstClass(obj);
	}

//	private static void handlerFirstClass(Object obj) {
//		new MockHandler().handler(obj.getClass());
//	}

	public static void resourcePreparation() {
		if(processInited) {
	        return;
	    }
		processInited = true;
		try {
			registerComponent();
			TestUtil launch = getInstance();
			launch.loadProp();
			LogbackUtil.resetLog();
			ScanUtil.loadAllClass();
			launch.processConfig();
		}catch(Error e) {
			log.error("resourcePreparation 异常",e);
			throw new JunitException(e);
		}
	}

	private static void registerComponent() {
		ClassScan.getInstance().register();
		PropResourceManager.getInstance().register();
		LazyBean.getInstance().register();
		BeanCreaterScan.getInstance().register();
		BeanFactoryProcessor.getInstance().register();
		LazyListableBeanFactory.getInstance().register();
		LazyApplicationContext.getInstance().register();
	}

	private void loadProp() {
		ConfigurableEnvironment cEnv = (ConfigurableEnvironment) getApplicationContext().getEnvironment();
		Properties properties = new Properties();
		for(String propPath : scanPropertiesList) {
			loadEnv(propPath, properties);
		}
		cEnv.getPropertySources().addLast(new PropertiesPropertySource("loadProp", properties));
	}

	public Boolean isScanClassPath(String cn) {
		return scanClassPath.stream().anyMatch(p -> cn.contains(p));
	}
	public void loadEnv(String propPath,String name) {
		ConfigurableEnvironment cEnv = (ConfigurableEnvironment) getApplicationContext().getEnvironment();
		Properties properties = new Properties();
		loadEnv(propPath, properties);
		if(name.contains("default")) {
			cEnv.getPropertySources().addLast(new PropertiesPropertySource(name, properties));
		}else {
			cEnv.getPropertySources().addFirst(new PropertiesPropertySource(name, properties));
		}
	}
	public void loadEnv(String propPath,Properties properties) {
		try {
			Resource propRes = ScanUtil.getRecourceAnyOne(propPath);
			if (propRes != null && propRes.exists()) {
				properties.load(propRes.getInputStream());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void register() {
		ContainerManager.registComponent(this);
	}

	@Override
	public String getBeanKey() {
		return JunitCoreComponentI.class.getSimpleName();
	}

}
