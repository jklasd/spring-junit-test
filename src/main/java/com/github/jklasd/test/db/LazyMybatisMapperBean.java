package com.github.jklasd.test.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import com.github.jklasd.test.AssemblyUtil;
import com.github.jklasd.test.InvokeUtil;
import com.github.jklasd.test.LazyBeanProcess;
import com.github.jklasd.test.LazyBeanProcess.LazyConfigProcess;
import com.github.jklasd.test.beanfactory.LazyBean;
import com.github.jklasd.test.beanfactory.LazyCglib;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.TestUtil;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang
 *
 */
@Slf4j
public class LazyMybatisMapperBean {
    private static volatile LazyMybatisMapperBean bean;
    public static LazyMybatisMapperBean getInstance() {
        if(bean == null) {
            bean = new LazyMybatisMapperBean();
        }
        return bean;
    }
	private static Object factory;
	public synchronized Object buildBean(Class<?> classBean) {
		try {
			return getMapper(classBean);
		} catch (Exception e) {
			log.error("获取Mapper", e);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation> mapperScanClass = ScanUtil
			.loadClass("org.mybatis.spring.annotation.MapperScan");

	public static final boolean useMybatis() {
		return factoryBeanClass != null;
	}

	public static final Class<? extends Annotation> getAnnotionClass() {
		if (mapperScanClass != null) {
			return mapperScanClass;
		}
		return null;
	}

//	private static ThreadLocal<SqlSession> sessionList = new ThreadLocal<>();
	private static final Class<?> factoryClass = ScanUtil.loadClass("org.apache.ibatis.session.SqlSessionFactory");
	private static final Class<?> factoryBeanClass = ScanUtil.loadClass("org.mybatis.spring.SqlSessionFactoryBean");
	private static final Class<?> sqlSessionTemplateClass = ScanUtil.loadClass("org.mybatis.spring.SqlSessionTemplate");
	
	private Object sqlSessionTemplate;
	public Object getSqlSessionTemplate() throws Exception{
	    if (factory == null) {
            buildMybatisFactory();
        }
	    if (sqlSessionTemplateClass != null && sqlSessionTemplate == null) {// 配置session控制器
            Constructor<?> structor = sqlSessionTemplateClass.getConstructor(factoryClass);
            sqlSessionTemplate = structor.newInstance(factory);
        }
        return sqlSessionTemplate;
    }

	private Object getMapper(Class<?> classBean) throws Exception {
		Object tag = InvokeUtil.invokeMethod(getSqlSessionTemplate(), "getMapper", classBean);
		return tag;
	}

	@SuppressWarnings("rawtypes")
	private void buildMybatisFactory() {
		if (factory == null) {
			Object obj = TestUtil.getApplicationContext().getBean(factoryBeanClass);
			if (obj != null) {
				try {
					factory = InvokeUtil.invokeMethod(obj, factoryBeanClass, "getObject");

//					Object dataSource= InvokeUtil.invokeMethod(InvokeUtil.invokeMethod(InvokeUtil.invokeMethod(factory, "getConfiguration"), 
//							"getEnvironment"), "getDataSource");
//					
//					Class AbstractRoutingDataSource = ScanUtil.loadClass("org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource");
//					if(ScanUtil.isExtends(dataSource.getClass(), AbstractRoutingDataSource)) {
//						InvokeUtil.invokeMethod(dataSource, "afterPropertiesSet");
//					}
				} catch (Exception e) {
					log.error("buildMybatisFactory#getObject", e);
				}
				return;
			} else {
				obj = TestUtil.getApplicationContext().getBean(factoryClass);
				if (obj != null) {
					factory = obj;
//					Object dataSource= InvokeUtil.invokeMethod(InvokeUtil.invokeMethod(InvokeUtil.invokeMethod(factory, "getConfiguration"), 
//							"getEnvironment"), "getDataSource");

//					Class AbstractRoutingDataSource = ScanUtil.loadClass("org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource");
//					if(ScanUtil.isExtends(dataSource.getClass(), AbstractRoutingDataSource)) {
//						InvokeUtil.invokeMethod(dataSource, "afterPropertiesSet");
//					}
					return;
				}
			}
			processAnnaForFactory();
		} else {
			log.debug("factory已存在");
		}
	}

	private void processAnnaForFactory() {
		if (factory == null) {
			AssemblyUtil param = new AssemblyUtil();
			param.setTagClass(factoryClass);
			factory = LazyBean.findCreateBeanFromFactory(param);
		}
	}

	public static void over() {
//		if(sessionList.get()!=null) {
//			sessionList.get().commit();
//			sessionList.get().close();
//			sessionList.remove();
//		}
	}

	private static List<String> mybatisScanPathList = Lists.newArrayList();

	private static Class<?> mapperScannerConfigurer = ScanUtil.loadClass("org.mybatis.spring.mapper.MapperScannerConfigurer");
	private static boolean loadScaned;
	public static boolean isMybatisBean(Class c) {
		if(useMybatis() && !loadScaned) {
			Object mybatisScan = LazyBean.buildProxy(mapperScannerConfigurer);
			try {
				Field cglibObjField= mybatisScan.getClass().getDeclaredField(LazyBean.PROXY_BEAN_FIELD);
				cglibObjField.setAccessible(true);
				LazyCglib obj = (LazyCglib) cglibObjField.get(mybatisScan);
				if(obj.getAttr().containsKey("basePackage")) {
					mybatisScanPathList.add(obj.getAttr().get("basePackage").toString());
					log.info("mybatisScanPathList=>{}",mybatisScanPathList);
				}
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
					| SecurityException e) {
				e.printStackTrace();
			}
			loadScaned = true;
		}
		Class mapperAnn = ScanUtil.loadClass("org.apache.ibatis.annotations.Mapper");
		if(mapperAnn!=null) {
		    if(c.getAnnotation(mapperAnn) != null) {
		        return true;
		    }
		}
		return !mybatisScanPathList.isEmpty() && mybatisScanPathList.stream()
				.anyMatch(mybatisScanPath -> c.getPackage().getName().contains(mybatisScanPath));
	}
//	private static Document cacheDocument;
//	public synchronized static void process(Element item, Document document) {
//		if(cacheDocument==null) {
//			cacheDocument = document;
//		}
//		NamedNodeMap attrs = item.getAttributes();
//		String className = attrs.getNamedItem("class").getNodeValue();
//		if(className.contains("MapperScannerConfigurer")) {
//			Map<String, Object> prop = XmlBeanUtil.loadXmlNodeProp(item.getChildNodes());
//			if(prop.containsKey("basePackage")) {
//				mybatisScanPathList.add(prop.get("basePackage").toString());
//			}
//		}else if(className.contains("SqlSessionFactoryBean")) {
//			//先不处理
//			factoryNode = item;
//		}
//	}

	public synchronized void processConfig(Class<?> configura, String[] packagePath) {
		mybatisScanPathList.addAll(Lists.newArrayList(packagePath));
	}

	private static void processAnnaForDataSource() {

	}

	public void configure() {
		// 判断是否存在类
		LazyBeanProcess.putAllMethod("org.mybatis.spring.SqlSessionFactoryBean", new LazyConfigProcess() {
			private Class<?> abstractRoutingDataSource = ScanUtil
					.loadClass("org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource");
			private boolean init = false;

			public void process(Object tagObj, Method method, Object[] param) {
				if (init)
					return;
				log.info("处理SqlSessionFactoryBean");
				try {
					Field dataSourceField = factoryBeanClass.getDeclaredField("dataSource");
					if (!dataSourceField.isAccessible()) {
						dataSourceField.setAccessible(true);
					}
					Object dataSource = dataSourceField.get(tagObj);
					if (ScanUtil.isExtends(dataSource.getClass(), abstractRoutingDataSource)) {
						InvokeUtil.invokeMethod(dataSource, "afterPropertiesSet");
					}
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
						| IllegalAccessException e) {
					log.error("dataSource#afterPropertiesSet", e);
				}
				init = true;
			}
		});
//		LazyBeanProcess.putAllMethod("com.alibaba.druid.pool.DruidDataSource", new LazyConfigProcess() {
//			private boolean init = false;
//
//			public void process(Object tagObj, Method method, Object[] param) {
//				if (init)
//					return;
//
//				log.info("DruidDataSource");
//
//				init = true;
//			}
//		});
	}

    
}