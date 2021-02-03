package com.junit.test;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.logging.logback.LogbackUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.junit.test.db.LazyMybatisMapperBean;
import com.junit.test.dubbo.LazyDubboBean;
import com.junit.test.spring.TestApplicationContext;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jubin.zhang
 *	2020-11-19
 *
 */
@Slf4j
public class TestUtil{
	private static Set<String> scanClassPath = Sets.newHashSet();
	public static void loadScanPath(String... scanPath) {
		for(String path : scanPath) {
			scanClassPath.add(path);
		}
	}
	public static List<String> xmlPathList = Lists.newArrayList();
	public static void loadXmlPath(String... xmlPath) {
		for(String path : xmlPath) {
			xmlPathList.add(path);
		}
	}
	private TestUtil() {
		log.info("--实例化TestUtil--");
	}
	private static ApplicationContext applicationContext;
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = new TestApplicationContext(applicationContext);
	}
	private void processConfig() {
		
		List<Class<?>> list = ScanUtil.findStaticMethodClass();
		log.debug("static class =>{}",list.size());
		/**
		 * 不能是抽象类
		 */
		list.stream().filter(classItem -> classItem != getClass() && !Modifier.isAbstract(classItem.getModifiers())).forEach(classItem->{
			log.debug("static class =>{}",classItem);
			LazyBean.processStatic(classItem);
		});
		
		xmlPathList.forEach(xml->readNode(xml));
	}

	private void readNode(String xml) {
		Resource file = applicationContext.getResource(xml);
		if(file!=null) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {
				//创建DocumentBuilder对象
				DocumentBuilder db = dbf.newDocumentBuilder();
				//通过DocumentBuilder对象的parser方法加载books.xml文件到当前项目下
				Document document = db.parse(file.getFile());
				NodeList nodeList = document.getElementsByTagName("import");
				for(int i = 0 ;i< nodeList.getLength();i++) {
					NamedNodeMap nodeMap = nodeList.item(i).getAttributes();
					Node attr = nodeMap.getNamedItem("resource");
					readNode(attr.getNodeValue());
				}
				NodeList beansList = document.getElementsByTagName("beans");
				if(beansList.getLength()>0) {
					NamedNodeMap nodeMap = beansList.item(0).getAttributes();
					Node attr = nodeMap.getNamedItem("xmlns:dubbo");
					if(attr!=null) {
						LazyDubboBean.processDubbo(document);
					}else {
						NodeList beanList = document.getElementsByTagName("bean");
						for(int i = 0 ;i< beanList.getLength();i++) {
							NamedNodeMap beanAttr = beanList.item(i).getAttributes();
							Node attr_ = beanAttr.getNamedItem("class");
							String className = attr_.getNodeValue();
							if(className.contains("org.mybatis")) {
								LazyMybatisMapperBean.process(beanList.item(i),document);
							}else {
								
							}
						}
					}
				}
			} catch (Exception e) {
			}
		}
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
		key = key.replace("${", "").replace("}", "");
		if(getStaticApplicationContext()!=null) {
			String[] keys = key.split(":");
			String value = getStaticApplicationContext().getEnvironment().getProperty(keys[0]);
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
			log.warn("转换类型异常{}==>{}",key,type);
			throw e;
		}
		
		return value;
	}
	
	public static PropertySources getPropertySource() {
		StandardEnvironment env = (StandardEnvironment) getStaticApplicationContext().getEnvironment();
		return env.getPropertySources();
	}

	public static void startTestForNoContainer(Object obj) {
		LazyBean.processAttr(obj, obj.getClass());
		TestUtil launch = new TestUtil();
		launch.setApplicationContext(null);
		Resource logback = applicationContext.getResource("logback.xml");
		if(logback != null) {
			LogbackUtil.init((StandardServletEnvironment) getStaticApplicationContext().getEnvironment());
			log.info("加载环境配置完毕");
		}
		ScanUtil.loadAllClass();
		launch.processConfig();
	}

	public static ApplicationContext getStaticApplicationContext() {
		return applicationContext;
	}

	public void setStaticApplicationContext(ApplicationContext staticApplicationContext) {
		this.applicationContext = staticApplicationContext;
	}
	
	public static Map<String,String> loadXmlNodeAttr(NamedNodeMap nodeMap){
		Map<String,String> map = Maps.newHashMap();
		if(nodeMap==null) {
			return map;
		}
		for(int i=0;i<nodeMap.getLength();i++) {
			map.put(nodeMap.item(i).getNodeName(), nodeMap.item(i).getNodeValue());
		}
		return map;
	}
	public static Map<String,Object> loadXmlNodeProp(NodeList list){
		Map<String,Object> map = Maps.newHashMap();
		one:for(int i=0;i<list.getLength();i++) {
			Map<String,String> prop = loadXmlNodeAttr(list.item(i).getAttributes());
			if(prop.containsKey("name")) {
				if(StringUtils.isNotBlank(prop.get("value"))) {
					map.put(prop.get("name"),prop.get("value"));
				}else if(StringUtils.isNotBlank(prop.get("ref"))){
					map.put(prop.get("name"),prop.get("ref"));
				}else if(StringUtils.isNotBlank(list.item(i).getNodeValue())){
					map.put(prop.get("name"),list.item(i).getNodeValue());
				}else if(list.item(i).hasChildNodes()) {
					NodeList nC = list.item(i).getChildNodes();
					List<Node> ll = Lists.newArrayList();
					for(int j=0;j<nC.getLength();j++) { // 读取一层关系
						Node tmpN = nC.item(j);
						String nN = tmpN.getNodeName();
						if(!nN.equals("#text")) {
							if(nN.equals("array")) {
								map.put(prop.get("name"), tmpN);
								continue one;
							}else {
								ll.add(tmpN);
							}
						}
					}
					map.put(prop.get("name"), ll);
				}
			}
		}
		return map;
	}
	public static Node getBeanById(Document document,String id){
		NodeList list = document.getElementsByTagName("bean");
		for(int i=0;i<list.getLength();i++) {
			Map<String,String> attr = loadXmlNodeAttr(list.item(i).getAttributes());
			if(Objects.equal(id, attr.get("id"))) {
				return list.item(i);
			}
		}
		return null;
	}
	public static Boolean isScanClassPath(String cn) {
		String tmpName = cn.replace("/", ".");
		return scanClassPath.stream().allMatch(p -> tmpName.contains(p));
	}
}
