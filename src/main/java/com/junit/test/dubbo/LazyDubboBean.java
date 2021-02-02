package com.junit.test.dubbo;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.google.common.collect.Maps;
import com.junit.test.ScanUtil;
import com.junit.test.TestUtil;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author jubin.zhang
 *
 */
@Slf4j
public class LazyDubboBean {
	@SuppressWarnings("rawtypes")
	private static Map<Class,Object> dubboData = Maps.newHashMap();
	@SuppressWarnings("rawtypes")
	private static Map<Class,Node> dubboNode = Maps.newHashMap();
	static {
		/**
		 * 加载dubboClass,两种方式，一种是注解引用。一种是读取dubboContext.xml
		 */
		try {
			readDubboClass();
		} catch (ParserConfigurationException | SAXException | IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	public static boolean isDubbo(Class<?> classBean) {
		return dubboNode.containsKey(classBean);
	}
	private static RegistryConfig registryConfig;
	public static Object buildBean(Class<?> dubboClass) {
		if(dubboData.containsKey(dubboClass)) {
			return dubboData.get(dubboClass);
		}
		log.info("构建DubboBean=>{}",dubboClass);
		ReferenceConfig<?> referenceConfig = new ReferenceConfig<>();
		referenceConfig.setInterface(dubboClass);
		NamedNodeMap nodeAttr = dubboNode.get(dubboClass).getAttributes();
		Node attr = nodeAttr.getNamedItem("group");
		String groupStr = attr.getNodeValue();
		if(groupStr.contains("$")) {
			groupStr = TestUtil.getPropertiesValue(groupStr.replace("${", "").replace("}", ""));
		}
		referenceConfig.setGroup(groupStr);
		ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-examples-consumer");
		if(registryConfig == null) {
			registryConfig = new RegistryConfig("zookeeper://" + TestUtil.getPropertiesValue("zookeeper.url"));
			registryConfig.setUsername(TestUtil.getPropertiesValue("zookeeper.username"));
			registryConfig.setPassword(TestUtil.getPropertiesValue("zookeeper.password"));
			registryConfig.setClient("curator");
			registryConfig.setSubscribe(true);
			registryConfig.setRegister(false);
		}
		referenceConfig.setApplication(applicationConfig);
		referenceConfig.setRegistry(registryConfig);
		Object obj = referenceConfig.get();
		dubboData.put(dubboClass,obj);
		return obj;
	}
	private static void readDubboClass() throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException {
		String xml = TestUtil.dubboXml;
		if(StringUtils.isBlank(xml)) {
			ScanUtil.findBeanByInterface(com.alibaba.dubbo.config.annotation.Service.class);
		}else {
			Resource[] resource = ScanUtil.getResources(xml);
			if(resource != null && resource.length>0) {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				//创建DocumentBuilder对象
				DocumentBuilder db = dbf.newDocumentBuilder();
				//通过DocumentBuilder对象的parser方法加载books.xml文件到当前项目下
				Document document = db.parse(resource[0].getFile());
				NodeList list = document.getElementsByTagName("dubbo:reference");
				log.info("dubbo:reference length =>{}",list.getLength());
				for(int i = 0 ;i< list.getLength();i++) {
					Node node = list.item(i);
					NamedNodeMap nodeMap = node.getAttributes();
					Node attr = nodeMap.getNamedItem("interface");
					String className = attr.getNodeValue();
					dubboNode.put(Class.forName(className), node);
				}
			}
		}
	}
}
