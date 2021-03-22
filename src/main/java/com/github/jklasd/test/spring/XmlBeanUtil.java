package com.github.jklasd.test.spring;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.jklasd.test.LazyBean;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.db.LazyMybatisMapperBean;
import com.github.jklasd.test.dubbo.LazyDubboBean;
import com.github.jklasd.test.mq.LazyRabbitMQBean;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XmlBeanUtil {
	public static List<String> xmlPathList = Lists.newArrayList();
//	public static DefaultBeanDefinitionDocumentReader dbddr = new DefaultBeanDefinitionDocumentReader();
	/**
	 * Resource resource, ProblemReporter problemReporter,
			ReaderEventListener eventListener, SourceExtractor sourceExtractor,
			XmlBeanDefinitionReader reader, NamespaceHandlerResolver namespaceHandlerResolver
	 */
//	private ProblemReporter ffpr = new FailFastProblemReporter();
//	private ReaderEventListener erel = new EmptyReaderEventListener();
//	private SourceExtractor ptse = new PassThroughSourceExtractor();
//	private NamespaceHandlerResolver  dnhr = new DefaultNamespaceHandlerResolver();
	
	private static Set<Class<?>> beanList = Sets.newHashSet();
	public static boolean containClass(Class<?> tag) {
		return beanList.contains(tag);
	}
	public static void loadXmlPath(String... xmlPath) {
		for(String path : xmlPath) {
			xmlPathList.add(path);
		}
	}
//	private BeanDefinitionParserDelegate bdpd;
//	private  void buildXmlReaderContext(String xml){
//		Resource resource = TestUtil.getApplicationContext().getResource(xml);
//		XmlBeanDefinitionReader  xbdr = new XmlBeanDefinitionReader((BeanDefinitionRegistry) TestUtil.getApplicationContext().getAutowireCapableBeanFactory());
//		
//		XmlReaderContext xrc = new XmlReaderContext(resource,ffpr,erel,ptse,xbdr,dnhr);
//		bdpd = new BeanDefinitionParserDelegate(xrc);
//	}
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
					Element attr = (Element) nodeList.item(i);
					readNode(attr.getAttribute("resource"));
				}
				NodeList beansList = document.getElementsByTagName("beans");
				if(beansList.getLength()>0) {
					Element nodeMap = (Element) beansList.item(0);
					if(nodeMap.hasAttribute("xmlns:dubbo")) {
						LazyDubboBean.processDubbo(document);
					}else {
						NodeList beanList = document.getElementsByTagName("bean");
						for(int i = 0 ;i< beanList.getLength();i++) {
							Element item = (Element)beanList.item(i);
							String className = item.getAttribute("class");
							if(className.contains("org.mybatis")) {
								LazyMybatisMapperBean.process(item,document);
							}else {
								//xml 注册Bean
								registerBean(className, item);
							}
						}
						
						NodeList contextList = document.getElementsByTagName("context:component-scan");
						if(contextList.getLength()>0) {
							Element contextAttr = (Element) contextList.item(0);//base-package
							TestUtil.loadScanPath(contextAttr.getAttribute("base-package"));
							ScanUtil.loadContextPathClass();
						}
						
						NodeList rabbitNodeList = document.getElementsByTagName("rabbit:connection-factory");
						if(rabbitNodeList.getLength()>0) {
							Element contextAttr = (Element) rabbitNodeList.item(0);
							LazyRabbitMQBean.loadConfig(contextAttr);
						}
					}
				}
				
			} catch (Exception e) {
			}
		}
	}
	
	private static void registerBean(String className, Element ele) {
//		util.bdpd.parseBeanDefinitionElement(attr);
		String beanName = null;
		if(ele.hasAttribute("id")) {
			beanName = ele.getAttribute("id");
		}
		if(ele.hasChildNodes()) {//查看是否有属性
			
		}
		try {
			Map<String, Object> attr = loadXmlNodeProp(ele.getChildNodes());
			log.info("attr=>{}",attr);
			Class<?> c = ScanUtil.loadClass(className);
			beanList.add(c);
			LazyBean.buildProxy(c, beanName,attr);
		} catch (Exception e) {
			log.error("registerBean",e);
		}
	}
	public static Map<String,Object> loadXmlNodeProp(NodeList list){
		Map<String,Object> map = Maps.newHashMap();
		one:for(int i=0;i<list.getLength();i++) {
			Node item = list.item(i);
			Element prop = null;
			if(item instanceof Element) {
				prop = (Element) item;
			}else {
				continue;
			}
			if(prop.hasAttribute("name")) {
				if(StringUtils.isNotBlank(prop.getAttribute("value"))) {
					map.put(prop.getAttribute("name"),prop.getAttribute("value"));
				}else if(StringUtils.isNotBlank(prop.getAttribute("ref"))){
					map.put(prop.getAttribute("name"),"ref:"+prop.getAttribute("ref"));
				}else if(StringUtils.isNotBlank(list.item(i).getNodeValue())){
					map.put(prop.getAttribute("name"),list.item(i).getNodeValue());
				}else if(list.item(i).hasChildNodes()) {
					NodeList nC = list.item(i).getChildNodes();
					List<Node> ll = Lists.newArrayList();
					for(int j=0;j<nC.getLength();j++) { // 读取一层关系
						Node tmpN = nC.item(j);
						if(tmpN instanceof Element) {
							String nN = tmpN.getNodeName();
							if(nN.equals("array")) {
								map.put(prop.getAttribute("name"), tmpN);
								continue one;
							}else if(nN.equals("map")){
//								ll.add(tmpN);
								Map<String,String> tmpMap = Maps.newHashMap();
								NodeList mNL = tmpN.getChildNodes();
								for(int m=0;m<mNL.getLength();m++) {
									Node tmpmN = mNL.item(m);
									if(tmpmN instanceof Element) {
										Element mprop = (Element) tmpmN;
										tmpMap.put(mprop.getAttribute("key"), mprop.getAttribute("value-ref"));
									}
								}
								map.put(prop.getAttribute("name"), tmpMap);
								continue one;
							}else {
								ll.add(tmpN);
							}
						}
					}
					map.put(prop.getAttribute("name"), ll);
				}
			}
		}
		return map;
	}
	public static Element getBeanById(Document document,String id){
		NodeList list = document.getElementsByTagName("bean");
		for(int i=0;i<list.getLength();i++) {
			Element item = (Element) list.item(i);
			if(Objects.equal(id, item.getAttribute("id"))) {
				return (Element) list.item(i);
			}
		}
		return null;
	}
	
//	public static Map<String,String> loadXmlNodeAttr(NamedNodeMap nodeMap){
//		Map<String,String> map = Maps.newHashMap();
//		if(nodeMap==null) {
//			return map;
//		}
//		for(int i=0;i<nodeMap.getLength();i++) {
//			map.put(nodeMap.item(i).getNodeName(), nodeMap.item(i).getNodeValue());
//		}
//		return map;
//	}
	
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
