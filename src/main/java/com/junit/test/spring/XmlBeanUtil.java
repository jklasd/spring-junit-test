package com.junit.test.spring;

import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.junit.test.TestUtil;
import com.junit.test.db.LazyMybatisMapperBean;
import com.junit.test.dubbo.LazyDubboBean;
import com.junit.test.mq.LazyRabbitMQBean;

public class XmlBeanUtil {
	public static List<String> xmlPathList = Lists.newArrayList();
	public static void loadXmlPath(String... xmlPath) {
		for(String path : xmlPath) {
			xmlPathList.add(path);
		}
	}
	public static void process() {
		xmlPathList.forEach(xml->readNode(xml));		
	}
	
	private static void readNode(String xml) {
		Resource file = TestUtil.getApplicationContext().getResource(xml);
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
						
						NodeList contextList = document.getElementsByTagName("context:component-scan");
						if(contextList.getLength()>0) {
							Map<String, String> contextAttr = loadXmlNodeAttr(contextList.item(0).getAttributes());//base-package
							TestUtil.loadScanPath(contextAttr.get("base-package"));
						}
						
						NodeList rabbitNodeList = document.getElementsByTagName("rabbit:connection-factory");
						if(rabbitNodeList.getLength()>0) {
							Map<String, String> contextAttr = loadXmlNodeAttr(rabbitNodeList.item(0).getAttributes());
							LazyRabbitMQBean.loadConfig(contextAttr);
						}
					}
				}
				
			} catch (Exception e) {
			}
		}
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
	
	public static List<Node> findNodeByTag(Node node,String tagName) {
		List<Node> list = Lists.newArrayList();
		NodeList nodeList = node.getChildNodes();
		for(int i=0; i<nodeList.getLength(); i++) {
			if(nodeList.item(i).getNodeName().equals(tagName)) {
				list.add(nodeList.item(i));
			}else {
				if(nodeList.item(i).hasChildNodes()) {
					list.addAll(findNodeByTag(nodeList.item(i), tagName));
				}
			}
		}
		return list;
	}
}
