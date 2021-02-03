package com.junit.test.dubbo;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.google.common.collect.Maps;
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
	private static Map<Class,Map<String,String>> dubboAttr = Maps.newHashMap();
	public static boolean isDubbo(Class<?> classBean) {
		return dubboAttr.containsKey(classBean);
	}
	private static RegistryConfig registryConfig;
	public static Object buildBean(Class<?> dubboClass) {
		if(dubboData.containsKey(dubboClass)) {
			return dubboData.get(dubboClass);
		}
		log.info("构建DubboBean=>{}",dubboClass);
		ReferenceConfig<?> referenceConfig = new ReferenceConfig<>();
		referenceConfig.setInterface(dubboClass);
		if(dubboAttr.get(dubboClass).containsKey("group")) {
			referenceConfig.setGroup(TestUtil.getPropertiesValue(dubboAttr.get(dubboClass).get("group")));
		}
		ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-examples-consumer");
		referenceConfig.setApplication(applicationConfig);
		referenceConfig.setRegistry(registryConfig);
		Object obj = referenceConfig.get();
		dubboData.put(dubboClass,obj);
		return obj;
	}
	public static void processDubbo(Document document) {
		processRegister(document.getElementsByTagName("dubbo:registry"));
		cacheReference(document.getElementsByTagName("dubbo:reference"));
	}
	private static void cacheReference(NodeList list) {
			for(int i = 0 ;i< list.getLength();i++) {
				Node node = list.item(i);
				NamedNodeMap nodeMap = node.getAttributes();
				Node attr = nodeMap.getNamedItem("interface");
				String className = attr.getNodeValue();
				try {
					dubboAttr.put(Class.forName(className), TestUtil.loadXmlNodeAttr(node.getAttributes()));
				} catch (Exception e) {
					log.error("",e);
				}
			}
	}
	private static void processRegister(NodeList elementsByTagName) {
		if(registryConfig == null) {
			Map<String,String> attr = TestUtil.loadXmlNodeAttr(elementsByTagName.item(0).getAttributes());
			String protocol="";
			if(!attr.containsKey("protocol") || attr.get("protocol").equals("zookeeper")) {
				protocol  = "zookeeper://";
			}
			registryConfig = new RegistryConfig(protocol+ TestUtil.getPropertiesValue(attr.get("address")));
			registryConfig.setUsername(TestUtil.getPropertiesValue(attr.get("username")));
			registryConfig.setPassword(TestUtil.getPropertiesValue(attr.get("password")));
			registryConfig.setClient(TestUtil.getPropertiesValue(attr.get("client")));
			registryConfig.setSubscribe(true);
			registryConfig.setRegister(false);
		}
	}
}
