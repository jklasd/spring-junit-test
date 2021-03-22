package com.github.jklasd.test.dubbo;

import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.github.jklasd.test.LazyBean;
import com.github.jklasd.test.TestUtil;
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
	private static Map<Class,Element> dubboRefferCache = Maps.newHashMap();
	private static Map<Class,Element> dubboServiceCache = Maps.newHashMap();
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
		if(dubboRefferCache.get(dubboClass).hasAttribute("group")) {
			referenceConfig.setGroup(TestUtil.getPropertiesValue(dubboRefferCache.get(dubboClass).getAttribute("group")));
		}
		if(dubboRefferCache.get(dubboClass).hasAttribute("timeout")) {
			referenceConfig.setTimeout(Integer.valueOf(TestUtil.getPropertiesValue(dubboRefferCache.get(dubboClass).getAttribute("timeout"),dubboRefferCache.get(dubboClass).getAttribute("timeout"))));
		}else {
			referenceConfig.setTimeout(10*1000);
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
			if(dubboServiceCache.get(dubboServiceClass).hasAttribute("group")) {
				serviceConfig.setGroup(TestUtil.getPropertiesValue(dubboServiceCache.get(dubboServiceClass).getAttribute("group")));
			}
			if(dubboServiceCache.get(dubboServiceClass).hasAttribute("timeout")) {
				serviceConfig.setTimeout(Integer.valueOf(TestUtil.getPropertiesValue(dubboServiceCache.get(dubboServiceClass).getAttribute("timeout"))));
			}
			serviceConfig.setRef(LazyBean.buildProxy(dubboServiceClass));
			serviceConfig.setRegistry(registryConfig);
			serviceConfig.export();
		}
	}
	private static void cacheService(NodeList serviceList) {
		for(int i = 0 ;i< serviceList.getLength();i++) {
			Element node = (Element) serviceList.item(i);
			String className = node.getAttribute("interface");
			try {
				dubboServiceCache.put(Class.forName(className),node);
			} catch (ClassNotFoundException e) {
				log.error("LazyDubboBean#cacheService=>{}",e.getMessage());
			}
		}
	}
	private static void cacheReference(NodeList list) {
			for(int i = 0 ;i< list.getLength();i++) {
				Element node = (Element) list.item(i);
				String className = node.getAttribute("interface");
				try {
					dubboRefferCache.put(Class.forName(className), node);
				} catch (Exception e) {
					log.error("",e);
				}
			}
	}
	private static void processRegister(NodeList elementsByTagName) {
		if(registryConfig == null) {
			Element attr = (Element) elementsByTagName.item(0);
			String protocol="";
			if(!attr.hasAttribute("protocol") || attr.getAttribute("protocol").equals("zookeeper")) {
				protocol  = "zookeeper://";
			}
			registryConfig = new RegistryConfig(protocol+ TestUtil.getPropertiesValue(attr.getAttribute("address")));
			registryConfig.setUsername(TestUtil.getPropertiesValue(attr.getAttribute("username")));
			registryConfig.setPassword(TestUtil.getPropertiesValue(attr.getAttribute("password")));
			registryConfig.setClient(TestUtil.getPropertiesValue(attr.getAttribute("client")));
			registryConfig.setSubscribe(true);
			registryConfig.setRegister(true);
		}
	}
	public static void putAnnService(Class<?> dubboServiceClass) {
		dubboServiceCache.put(dubboServiceClass,new Element() {
			
			@Override
			public Object setUserData(String key, Object data, UserDataHandler handler) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void setTextContent(String textContent) throws DOMException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setPrefix(String prefix) throws DOMException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setNodeValue(String nodeValue) throws DOMException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Node removeChild(Node oldChild) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void normalize() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public String lookupPrefix(String namespaceURI) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String lookupNamespaceURI(String prefix) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public boolean isSupported(String feature, String version) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isSameNode(Node other) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isEqualNode(Node arg) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isDefaultNamespace(String namespaceURI) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public Node insertBefore(Node newChild, Node refChild) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public boolean hasChildNodes() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean hasAttributes() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public Object getUserData(String key) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getTextContent() throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Node getPreviousSibling() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getPrefix() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Node getParentNode() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Document getOwnerDocument() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getNodeValue() throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public short getNodeType() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public String getNodeName() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Node getNextSibling() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getNamespaceURI() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getLocalName() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Node getLastChild() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Node getFirstChild() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Object getFeature(String feature, String version) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public NodeList getChildNodes() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getBaseURI() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public NamedNodeMap getAttributes() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public short compareDocumentPosition(Node other) throws DOMException {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public Node cloneNode(boolean deep) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Node appendChild(Node newChild) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setIdAttribute(String name, boolean isId) throws DOMException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Attr setAttributeNode(Attr newAttr) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setAttribute(String name, String value) throws DOMException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void removeAttribute(String name) throws DOMException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean hasAttribute(String name) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public String getTagName() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public TypeInfo getSchemaTypeInfo() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public NodeList getElementsByTagName(String name) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Attr getAttributeNode(String name) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getAttributeNS(String namespaceURI, String localName) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getAttribute(String name) {
				// TODO Auto-generated method stub
				return null;
			}
		});
	}
}
