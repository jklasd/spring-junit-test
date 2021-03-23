package com.github.jklasd.test.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.github.jklasd.test.AssemblyUtil;
import com.github.jklasd.test.InvokeUtil;
import com.github.jklasd.test.LazyBean;
import com.github.jklasd.test.LazyBeanProcess;
import com.github.jklasd.test.LazyBeanProcess.LazyConfigProcess;
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
public class LazyMybatisMapperBean{
//	private static DataSource dataSource;
	private static Object factory;
	
	
	public synchronized static Object buildBean(Class<?> classBean) {
		try {
			if(factory == null){
				buildMybatisFactory();
			}
			return getMapper(classBean);
		} catch (Exception e) {
			log.error("获取Mapper",e);
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation> mapperScanClass =  ScanUtil.loadClass("org.mybatis.spring.annotation.MapperScan");
	public static final Class<? extends Annotation> getAnnotionClass() {
		if(mapperScanClass!=null) {
			return mapperScanClass;
		}
		return null;
	}
//	private static ThreadLocal<SqlSession> sessionList = new ThreadLocal<>();
	private static final Class<?> factoryClass =  ScanUtil.loadClass("org.apache.ibatis.session.SqlSessionFactory");
	private static final Class<?> factoryBeanClass =  ScanUtil.loadClass("org.mybatis.spring.SqlSessionFactoryBean");
	private static final Class<?> sqlSessionTemplateClass =  ScanUtil.loadClass("org.mybatis.spring.SqlSessionTemplate");
	private static Object sqlSessionTemplate;
	private static Object getMapper(Class<?> classBean) throws Exception {
		if(sqlSessionTemplateClass!=null && sqlSessionTemplate ==null) {//配置session控制器
			Constructor<?> structor = sqlSessionTemplateClass.getConstructor(factoryClass);
			sqlSessionTemplate = structor.newInstance(factory);
		}
		Object tag = InvokeUtil.invokeMethod(sqlSessionTemplate, "getMapper", classBean);
		return tag;
	}

	@SuppressWarnings("rawtypes")
	private static void buildMybatisFactory(){
		if(factory == null) {
			Object obj = TestUtil.getApplicationContext().getBean(factoryBeanClass);
			if(obj!=null) {
				try {
					factory = InvokeUtil.invokeMethod(obj,factoryBeanClass,"getObject");
					
//					Object dataSource= InvokeUtil.invokeMethod(InvokeUtil.invokeMethod(InvokeUtil.invokeMethod(factory, "getConfiguration"), 
//							"getEnvironment"), "getDataSource");
//					
//					Class AbstractRoutingDataSource = ScanUtil.loadClass("org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource");
//					if(ScanUtil.isExtends(dataSource.getClass(), AbstractRoutingDataSource)) {
//						InvokeUtil.invokeMethod(dataSource, "afterPropertiesSet");
//					}
				} catch (Exception e) {
					log.error("buildMybatisFactory#getObject",e);
				}
				return;
			}else {
				obj = TestUtil.getApplicationContext().getBean(factoryClass);
				if(obj!=null) {
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
		}else {
			log.debug("factory已存在");
		}
	}


	private static void processAnnaForFactory() {
		if(factory == null) {
			AssemblyUtil param = new AssemblyUtil();
			param.setTagClass(factoryClass);
			factory = LazyBean.findCreateBeanFromFactory(param);
		}
	}


//	public static DataSource buildDataSource(String id) {
//		if(dataSource == null) {
//			if(cacheDocument != null) {
//				Object obj = LazyBean.buildProxy(DataSource.class);
//				if(obj != null) {
//					dataSource = (DataSource) obj;
//				}else {
//					processXmlForDataSource(id);
//				}
//			}else {
//				//查询注解方式
//				processAnnaForDataSource();
//			}
//		}
//		return dataSource;
//	}

//	private static Object processXmlCreateDS(String id) {
//		Element dataSourceNode = XmlBeanUtil.getBeanById(cacheDocument, id);
//		try {
//			Class<?> dataSourceC = Class.forName(dataSourceNode.getAttribute("class"));
//			Object obj = dataSourceC.newInstance();
//			Map<String, Object> dataSourceProp = XmlBeanUtil.loadXmlNodeProp(dataSourceNode.getChildNodes());
//			dataSourceProp.keySet().forEach(field -> {
//				try {
//					log.debug("{}=>{}", field, dataSourceProp.get(field.toString()));
//					LazyBean.setAttr(field, obj, dataSourceC, dataSourceProp.get(field.toString()));
//				} catch (SecurityException e) {
//					log.error("buildDataSource", e);
//				}
//			});
////			if(dataSourceAttr.containsKey("init-method")) {
////				Method init = dataSourceC.getDeclaredMethod(dataSourceAttr.get("init-method"));
////				init.invoke(obj);
////			}
//			return obj;
//		} catch (Exception e) {
//			return null;
//		}
//	}

//	private static void processXmlForDataSource(String id) {
//		Element dataSourceAttr = XmlBeanUtil.getBeanById(cacheDocument, id);
//		try {
//			Class<?> dataSourceC = Class.forName(dataSourceAttr.getAttribute("class"));
//			Object obj = dataSourceC.newInstance();
//			Map<String, Object> dataSourceProp = XmlBeanUtil.loadXmlNodeProp(dataSourceAttr.getChildNodes());
//			dataSourceProp.keySet().forEach(field ->{
//				try {
//					log.debug("{}=>{}",field,dataSourceProp.get(field.toString()));
//					LazyBean.setAttr(field,obj, dataSourceC, dataSourceProp.get(field.toString()));
//				} catch (SecurityException e) {
//					log.error("buildDataSource",e);
//				}
//			});
//			try {
//				Class AbstractRoutingDataSource = ScanUtil.loadClass("org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource");
//				if(ScanUtil.isExtends(dataSourceC, AbstractRoutingDataSource)) {
//					Map<String,Object> dataSource = Maps.newHashMap();
//					Map<String,String> targetDataSources = (Map<String, String>) dataSourceProp.get("targetDataSources");
//					targetDataSources.keySet().forEach(key ->{
//						DataSource ds = (DataSource) processXmlCreateDS(targetDataSources.get(key));
//						dataSource.put(key, ds);
//					});
//					LazyBean.setAttr("targetDataSources",obj, dataSourceC, dataSource);
//					Method afterPropertiesSet = AbstractRoutingDataSource.getDeclaredMethod("afterPropertiesSet");
//					afterPropertiesSet.invoke(obj);
//				}
//			} catch (Exception e) {
//			}
//			dataSource = (DataSource) obj;
//		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
//			log.error("buildDataSource",e);
//		}
//	}


	public static void over() {
//		if(sessionList.get()!=null) {
//			sessionList.get().commit();
//			sessionList.get().close();
//			sessionList.remove();
//		}
	}


	private static List<String> mybatisScanPathList = Lists.newArrayList();
	public static boolean isMybatisBean(Class c) {
		return !mybatisScanPathList.isEmpty() 
				&& mybatisScanPathList.stream().anyMatch(mybatisScanPath->c.getPackage().getName().contains(mybatisScanPath));
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


	public synchronized static void processConfig(Class<?> configura, String[] packagePath) {
		mybatisScanPathList.addAll(Lists.newArrayList(packagePath));
	}

	private static void processAnnaForDataSource() {
		
	}


	public static void processAttr(String className, Map<String, Object> attr) {
		if(className.equals("org.mybatis.spring.mapper.MapperScannerConfigurer") 
				&& attr.containsKey("basePackage")) {
			mybatisScanPathList.add(attr.get("basePackage").toString());
		}
	}


	public static void configure() {
		//判断是否存在类
		LazyBeanProcess.putAllMethod("org.mybatis.spring.SqlSessionFactoryBean",new LazyConfigProcess() {
			private boolean init = false;
			public void process(Object tagObj, Method method, Object[] param) {
				if(init)
					return;
				log.info("处理SqlSessionFactoryBean");
				try {
					Field dataSourceField = factoryBeanClass.getDeclaredField("dataSource");
					if(!dataSourceField.isAccessible()) {
						dataSourceField.setAccessible(true);
					}
					Object dataSource = dataSourceField.get(tagObj);
					InvokeUtil.invokeMethod(dataSource, "afterPropertiesSet");
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					log.error("dataSource#afterPropertiesSet",e);
				}
				init = true;
			}
		});
		LazyBeanProcess.putAllMethod("com.alibaba.druid.pool.DruidDataSource", new LazyConfigProcess() {
			private boolean init = false;
			public void process(Object tagObj, Method method, Object[] param) {
				if(init)
					return;
				
				log.info("DruidDataSource");
				
				init = true;
			}
		});
	}
}