package com.github.jklasd.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.abstrac.JunitApplicationContext;
import com.github.jklasd.test.common.component.ClassAnnComponent;
import com.github.jklasd.test.common.component.ScannerRegistrarComponent;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.interf.register.JunitCoreComponentI;
import com.github.jklasd.test.common.util.JunitConver;
import com.github.jklasd.test.common.util.LogbackUtil;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.core.facade.JunitResourceLoaderManager;
import com.github.jklasd.test.core.facade.processor.BeanFactoryProcessor;
import com.github.jklasd.test.core.facade.scan.BeanCreaterScan;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.core.facade.scan.PropResourceManager;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.github.jklasd.test.lazyplugn.spring.ObjectProviderFactory;
import com.google.common.collect.Lists;
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
	    ContainerManager.registComponent(bean);
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
	    JunitResourceLoaderManager.getInstance().initLoader();
		ScannerRegistrarComponent.process();
		ClassAnnComponent.afterScan();
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
	ExpressionParser parser = new SpelExpressionParser();
	TemplateParserContext ctx = new TemplateParserContext();

	public Object valueFromEnvForAnnotation(String key, Type type) {
		String value = getApplicationContext().getEnvironment().resolvePlaceholders(key);
		if (value.startsWith(ctx.getExpressionPrefix()) && value.endsWith(ctx.getExpressionSuffix())) {
			if(type instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) type;
				Object result = parser.parseExpression(value,ctx).getValue();
				return converType(result,pt);
			}else {
				Object result = parser.parseExpression(value,ctx).getValue((Class<?>)type);
				return result;
			}
        }
		try {
			if (StringUtils.isNotBlank(value)) {
				if (type == char.class) {
					return value.charAt(0);
				} else if (type == Class.class) {
                    return ScanUtil.loadClass(value);
                } else{
                	return JunitConver.converValue(value, (Class<?>)type);
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

	private Object converType(Object result, ParameterizedType pt) {
		if(pt.getRawType() == List.class) {
			if (result instanceof String[]) {
				return Lists.newArrayList((String[])result);
			}else if (result instanceof Byte[] || result instanceof byte[]) {
				return Lists.newArrayList((Byte[])result);
			}else if (result instanceof Integer[] || result instanceof int[]) {
				return Lists.newArrayList((Integer[])result);
			}else if (result instanceof Long[] || result instanceof long[]) {
				return Lists.newArrayList((Long[])result);
			}else if (result instanceof Double[] || result instanceof double[]) {
				return Lists.newArrayList((Double[])result);
			}else if (result instanceof BigDecimal[]) {
				return Lists.newArrayList((BigDecimal[])result);
			}else if (result instanceof Boolean[] || result instanceof boolean[]) {
				return Lists.newArrayList((Boolean[])result);
			}else if (result instanceof char[]) {
				return Lists.newArrayList((char[])result);
			} else{
				log.info("TestUil converType=========其他类型========={}=",pt);
			}
		}
		return null;
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
		if(isProcessInited()) {
	        return;
	    }
		processInited = true;
		try {
			TestUtil launch = getInstance();
			registerComponent();
			launch.loadProp();
			LogbackUtil.resetLog();
			ScanUtil.loadAllClass();
			launch.processConfig();
		}catch(Error e) {
			log.error("resourcePreparation 异常",e);
			throw new JunitException(e);
		}
	}
	/**
	 * 方便引用jar的代码中用到组件
	 */
	private static void registerComponent() {
		ContainerManager.registComponent( ClassScan.getInstance());
		ContainerManager.registComponent( PropResourceManager.getInstance());
		ContainerManager.registComponent( LazyBean.getInstance());
		ContainerManager.registComponent( BeanCreaterScan.getInstance());
		ContainerManager.registComponent( BeanFactoryProcessor.getInstance());
		try {
			ContainerManager.createAndregistComponent(ObjectProviderFactory.class);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
//		LazyListableBeanFactory.getInstance().register();
//		LazyApplicationContext.getInstance().register();
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
	public String getBeanKey() {
		return JunitCoreComponentI.class.getSimpleName();
	}

	public static boolean isProcessInited() {
		return processInited;
	}

}
