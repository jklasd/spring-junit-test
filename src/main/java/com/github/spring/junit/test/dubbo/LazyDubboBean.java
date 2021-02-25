package com.github.spring.junit.test.dubbo;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.github.spring.junit.test.LazyBean;
import com.github.spring.junit.test.TestUtil;
import com.github.spring.junit.test.spring.XmlBeanUtil;
import com.google.common.collect.Maps;

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
	private static Map<Class,Map<String,String>> dubboRefferCache = Maps.newHashMap();
	private static Map<Class,Map<String,String>> dubboServiceCache = Maps.newHashMap();
	public static boolean isDubbo(Class<?> classBean) {
		return dubboRefferCache.containsKey(classBean);
	}
	private static RegistryConfig registryConfig;
	public static Object buildBean(Class<?> dubboClass) {
		if(dubboData.containsKey(dubboClass)) {
			return dubboData.get(dubboClass);
		}
		log.info("构建Dubbo 代理服务=>{}",dubboClass);
		ReferenceConfig<?> referenceConfig = new ReferenceConfig<>();
		referenceConfig.setInterface(dubboClass);
		if(dubboRefferCache.get(dubboClass).containsKey("group")) {
			referenceConfig.setGroup(TestUtil.getPropertiesValue(dubboRefferCache.get(dubboClass).get("group")));
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
		cacheService(document.getElementsByTagName("dubbo:service"));
		//dubbo:protocol 待处理
	}
	
	public static void registerDubboService(Class<?> dubboServiceClass) {
		if(dubboServiceCache.containsKey(dubboServiceClass)) {
			log.info("注册dubboService=>{}",dubboServiceClass);
			ServiceConfig<Object>  serviceConfig = new ServiceConfig<>();
			serviceConfig.setApplication(new ApplicationConfig("dubbo-examples-service"));
			serviceConfig.setInterface(dubboServiceClass);
			if(dubboServiceCache.get(dubboServiceClass).containsKey("group")) {
				serviceConfig.setGroup(TestUtil.getPropertiesValue(dubboServiceCache.get(dubboServiceClass).get("group")));
			}
			if(dubboServiceCache.get(dubboServiceClass).containsKey("timeout")) {
				serviceConfig.setTimeout(Integer.valueOf(TestUtil.getPropertiesValue(dubboServiceCache.get(dubboServiceClass).get("timeout"))));
			}
			serviceConfig.setRef(LazyBean.buildProxy(dubboServiceClass));
			serviceConfig.setRegistry(registryConfig);
			serviceConfig.export();
		}
	}
	private static void cacheService(NodeList serviceList) {
		for(int i = 0 ;i< serviceList.getLength();i++) {
			Node node = serviceList.item(i);
			Map<String,String> attr = XmlBeanUtil.loadXmlNodeAttr(node.getAttributes());
			String className = attr.get("interface");
			try {
				dubboServiceCache.put(Class.forName(className),attr);
			} catch (ClassNotFoundException e) {
				log.error("LazyDubboBean#cacheService=>{}",e.getMessage());
			}
		}
	}
	private static void cacheReference(NodeList list) {
			for(int i = 0 ;i< list.getLength();i++) {
				Node node = list.item(i);
				Map<String,String> attr = XmlBeanUtil.loadXmlNodeAttr(node.getAttributes());
				String className = attr.get("interface");
				try {
					dubboRefferCache.put(Class.forName(className), attr);
				} catch (Exception e) {
					log.error("",e);
				}
			}
	}
	private static void processRegister(NodeList elementsByTagName) {
		if(registryConfig == null) {
			Map<String,String> attr = XmlBeanUtil.loadXmlNodeAttr(elementsByTagName.item(0).getAttributes());
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
