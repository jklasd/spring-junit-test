package com.github.jklasd.test.db;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.jklasd.test.AssemblyUtil;
import com.github.jklasd.test.LazyBean;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.spring.XmlBeanUtil;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author jubin.zhang
 *
 */
@Slf4j
public class LazyMybatisMapperBean{
	private static DataSource dataSource;
	private static SqlSessionFactory factory;
	
	
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
	
	private static ThreadLocal<SqlSession> sessionList = new ThreadLocal<>();
	
	private static SqlSessionTemplate sqlSessionTemplate;
	
	private static Object getMapper(Class<?> classBean) throws Exception {
		if(sqlSessionTemplate==null) {//配置session控制器
			sqlSessionTemplate = new SqlSessionTemplate(factory);
		}
//		if(sessionList.get() != null){
//			return sessionList.get().getMapper(classBean);
//		}else {
//			sessionList.set(factory.openSession());
//		}
//		Object tag = sessionList.get().getMapper(classBean);
		Object tag = sqlSessionTemplate.getMapper(classBean);
		return tag;
	}

	private static Node factoryNode;
	private static void buildMybatisFactory(){
		if(factory == null) {
			if(factoryNode!=null) {
				processXmlForFactory();
			}else {
				processAnnaForFactory();
			}
		}else {
			log.debug("factory已存在");
		}
	}


	private static void processAnnaForFactory() {
		if(factory == null) {
			AssemblyUtil param = new AssemblyUtil();
			param.setTagClass(SqlSessionFactory.class);
			factory = (SqlSessionFactory) ScanUtil.findCreateBeanFromFactory(param);
		}
	}


	private static void processXmlForFactory() {
		SqlSessionFactoryBean factoryTmp = new SqlSessionFactoryBean();
		try {
			Map<String, Object> prop = XmlBeanUtil.loadXmlNodeProp(factoryNode.getChildNodes());
			Resource[] resources = ScanUtil.getResources(prop.get("mapperLocations").toString());
			factoryTmp.setMapperLocations(resources);
			factoryTmp.setDataSource(buildDataSource(prop.get("dataSource").toString()));
			if(prop.containsKey("plugins")) {
				List<Interceptor> listPlugins = Lists.newArrayList();
				if(prop.get("plugins") instanceof Node) {
					Node plugins = (Node) prop.get("plugins");
					if(plugins.getNodeName().equals("array")) {
						NodeList beans = plugins.getChildNodes();
						for(int i=0;i<beans.getLength();i++) {
							Node n = beans.item(i);
							if(n.getNodeName().equals("#text")) {
								continue;
							}
							String pluginClass = XmlBeanUtil.loadXmlNodeAttr(n.getAttributes()).get("class");
							Class<?> tmp = Class.forName(pluginClass);
							listPlugins.add((Interceptor) tmp.newInstance());
						}
					}
				}else if(prop.get("plugins") instanceof List) {
					
				}
				Interceptor[] is = listPlugins.toArray(new Interceptor[0]);
				factoryTmp.setPlugins(is);
			}
			factoryTmp.afterPropertiesSet();
			factory = factoryTmp.getObject();
		} catch (Exception e) {
			log.error("buildMybatisFactory",e);
		}
	}


	public static DataSource buildDataSource(String id) {
		if(dataSource == null) {
			if(cacheDocument != null) {
				processXmlForDataSource(id);
			}else {
				//查询注解方式
				processAnnaForDataSource();
			}
		}
		return dataSource;
	}


	private static void processXmlForDataSource(String id) {
		Node dataSourceNode = XmlBeanUtil.getBeanById(cacheDocument, id);
		Map<String,String> dataSourceAttr = XmlBeanUtil.loadXmlNodeAttr(dataSourceNode.getAttributes());
		try {
			Class<?> dataSourceC = Class.forName(dataSourceAttr.get("class"));
			Object obj = dataSourceC.newInstance();
			Map<String, Object> dataSourceProp = XmlBeanUtil.loadXmlNodeProp(dataSourceNode.getChildNodes());
			dataSourceProp.keySet().forEach(field ->{
				try {
					log.debug("{}=>{}",field,dataSourceProp.get(field.toString()));
					LazyBean.setAttr(field,obj, dataSourceC, dataSourceProp.get(field.toString()));
				} catch (SecurityException e) {
					log.error("buildDataSource",e);
				}
			});
			dataSource = (DataSource) obj;
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			log.error("buildDataSource",e);
		}
	}


	public static void over() {
		if(sessionList.get()!=null) {
			sessionList.get().commit();
			sessionList.get().close();
			sessionList.remove();
		}
	}


	private static List<String> mybatisScanPathList = Lists.newArrayList();
	public static boolean isMybatisBean(Class c) {
		return !mybatisScanPathList.isEmpty() 
				&& mybatisScanPathList.stream().anyMatch(mybatisScanPath->c.getPackage().getName().contains(mybatisScanPath));
	}
	private static Document cacheDocument;
	public synchronized static void process(Node item, Document document) {
		if(cacheDocument==null) {
			cacheDocument = document;
		}
		NamedNodeMap attrs = item.getAttributes();
		String className = attrs.getNamedItem("class").getNodeValue();
		if(className.contains("MapperScannerConfigurer")) {
			Map<String, Object> prop = XmlBeanUtil.loadXmlNodeProp(item.getChildNodes());
			if(prop.containsKey("basePackage")) {
				mybatisScanPathList.add(prop.get("basePackage").toString());
			}
		}else if(className.contains("SqlSessionFactoryBean")) {
			//先不处理
			factoryNode = item;
		}
	}


	public synchronized static void processConfig(Class<?> configura, String[] packagePath) {
		// TODO Auto-generated method stub
		mybatisScanPathList.addAll(Lists.newArrayList(packagePath));
	}

	private static void processAnnaForDataSource() {
		
	}
}