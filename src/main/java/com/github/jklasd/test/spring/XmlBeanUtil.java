package com.github.jklasd.test.spring;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.jklasd.test.LazyBean;
import com.github.jklasd.test.LazyBeanProcess.LazyBeanInitProcess;
import com.github.jklasd.test.LazyBeanProcess.LazyBeanInitProcessImpl;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.TestUtil;
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

	private static Set<Class<?>> beanList = Sets.newHashSet();

	public static boolean containClass(Class<?> tag) {
		return beanList.contains(tag);
	}

	public static void loadXmlPath(String... xmlPath) {
		for (String path : xmlPath) {
			xmlPathList.add(path);
		}
	}

	public static void process() {
		xmlPathList.forEach(xml -> readNode(xml));
	}

	private static void readNode(String xml) {
		Resource file = TestUtil.getApplicationContext().getResource(xml);
		if (file != null) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {
				// 创建DocumentBuilder对象
				DocumentBuilder db = dbf.newDocumentBuilder();
				// 通过DocumentBuilder对象的parser方法加载books.xml文件到当前项目下
				Document document = db.parse(file.getFile());
				NodeList nodeList = document.getElementsByTagName("import");
				for (int i = 0; i < nodeList.getLength(); i++) {
					Element attr = (Element) nodeList.item(i);
					readNode(attr.getAttribute("resource"));
				}
				NodeList beansList = document.getElementsByTagName("beans");
				if (beansList.getLength() > 0) {
					Element nodeMap = (Element) beansList.item(0);
					if (nodeMap.hasAttribute("xmlns:dubbo")) {
						LazyDubboBean.processDubbo(document);
					} else {
						NodeList beanList = document.getElementsByTagName("bean");
						if(beanList.getLength()>0) {
							registerBean(beanList);
						}

						NodeList contextList = document.getElementsByTagName("context:component-scan");
						if (contextList.getLength() > 0) {
							Element contextAttr = (Element) contextList.item(0);// base-package
							TestUtil.loadScanPath(contextAttr.getAttribute("base-package"));
							ScanUtil.loadContextPathClass();
						}

						NodeList rabbitNodeList = document.getElementsByTagName("rabbit:connection-factory");
						if (rabbitNodeList.getLength() > 0) {
							Element contextAttr = (Element) rabbitNodeList.item(0);
							LazyRabbitMQBean.getInstance().loadConfig(contextAttr);
						}
					}
				}

			} catch (Exception e) {
				log.error("加载xml",e);
			}
		}
	}

	private static void registerBean(NodeList beanList) {
		Map<String,LazyBeanInitProcessImpl> tmpAttrMap = Maps.newHashMap();
		int size = beanList.getLength();
		log.info("bean size:{}",size);
		for (int i = 0; i < size; i++) {
			Element item = (Element) beanList.item(i);
			String className = item.getAttribute("class");
			registerBean(className, item,tmpAttrMap);
		}
		for (int i = 0; i < size; i++) {
			Element item = (Element) beanList.item(i);
			String className = item.getAttribute("class");
			processBeanAttr(className, item ,tmpAttrMap);
//			log.info("i ->{}",i);
		}
	}

	private static void processBeanAttr(String className,Element ele, Map<String, LazyBeanInitProcessImpl> tmpAttrMap) {
//		if(className.equals("org.mybatis.spring.mapper.MapperScannerConfigurer")) {
//			log.info("断点");
//		}
		String beanName = "";
		if (ele.hasAttribute("id")) {
			beanName = ele.getAttribute("id");
		}
		Class<?> eleClass = ScanUtil.loadClass(className);
		beanName = beanName==""?LazyBean.getBeanName(eleClass):beanName;
		String key = className+"-"+beanName;
//		log.info(className);
		Map<String, Object> attr = loadXmlNodeProp(ele.getChildNodes());
		loadXmlNodeProp2(attr,tmpAttrMap);
		processValue(attr, eleClass);
		processAttr(className, attr);
		LazyBeanInitProcess processer = tmpAttrMap.get(key).getProcess();
//		if(ele.hasAttribute("init-method")) {
//			Map<String,String> method = Maps.newHashMap();
//			method.put("init-method", ele.getAttribute("init-method"));
//			processer.initMethod(method);
//		}
		processer.init(attr);
	}


	private static Object registerBean(String className, Element ele, Map<String, LazyBeanInitProcessImpl> tmpAttrMap) {
		String beanName = "";
		if (ele.hasAttribute("id")) {
			beanName = ele.getAttribute("id");
		}
		try {
			Object obj = null;
			Class<?> c = ScanUtil.loadClass(className);
			beanName = beanName==""?LazyBean.getBeanName(c):beanName;
			if((obj = TestUtil.getApplicationContext().getBean(beanName)) != null) {
				return obj;
			}
			beanList.add(c);
			String key = className +"-" + beanName;
			if(!tmpAttrMap.containsKey(key)) {
				tmpAttrMap.put(key, new LazyBeanInitProcessImpl());
			}
			obj = LazyBean.buildProxy(c, beanName, tmpAttrMap.get(key));
			TestUtil.getApplicationContext().registBean(beanName, obj, c);
			return obj;
		} catch (Exception e) {
			log.error("registerBean", e);
		}
		return null;
	}

	private static void processValue(Map<String, Object> attr, Class<?> tabClass) {
		Map<String,Boolean> finded = Maps.newHashMap();
		Map<String,Boolean> sameType = Maps.newHashMap();
		attr.keySet().forEach(field -> {
			Object val = attr.get(field);
			String mName = "set" + field.substring(0, 1).toUpperCase() + field.substring(1);
			Method[] methods = tabClass.getDeclaredMethods();
			for (Method m : methods) {
				if (Objects.equal(m.getName(), mName)) {
					Class<?> paramType = m.getParameterTypes()[0];
					if (val.getClass() != paramType
							&& !ScanUtil.isImple(val.getClass(), paramType)
							&& !ScanUtil.isBasicClass(paramType)) {
						Object obj = processTypeValue(field, val, paramType);
						if(obj!=null) {
							attr.put(field, obj);
							finded.put(field, true);
						}
					}else {
						sameType.put(field, true);
					}
					return;
				}
			}
			Field[] fields = tabClass.getDeclaredFields();
			for (Field f : fields) {
				if (Objects.equal(f.getName(), field)) {
					if (val.getClass() != field.getClass()
							&& !ScanUtil.isImple(val.getClass(), field.getClass())
							&& !ScanUtil.isBasicClass(field.getClass())) {
						Object obj = processTypeValue(field, val, field.getClass());
						if(obj!=null) {
							attr.put(field, obj);
							finded.put(field, true);
						}
					}else {
						sameType.put(field, true);
					}
					return;
				}
			}
		});
		if(tabClass.getSuperclass()!=null && attr.size()>finded.size()+sameType.size()) {
			Map<String, Object> tmpAttr = Maps.newHashMap();
			attr.keySet().stream().filter(k->!finded.containsKey(k) && !sameType.containsKey(k)).forEach(k->{
				tmpAttr.put(k, attr.get(k));
			});
			processValue(tmpAttr, tabClass.getSuperclass());
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
    private static Object processTypeValue(String field, Object val, Class<?> paramType) {
		try {
			if (ScanUtil.isImple(paramType, Map.class)) {
				Object prop = paramType.newInstance();
				if(val.toString().contains("=")) {
					String[] k_v = val.toString().split("=");
					((Map) prop).put(k_v[0], k_v[1]);
					return prop;
				}
			}else if (paramType == Map.class) {
				Object prop = Maps.newHashMap();
				String[] k_v = val.toString().split("=");
				((Map) prop).put(k_v[0], k_v[1]);
				return prop;
			} else if (!ScanUtil.isBasicClass(paramType)) {
				if (paramType.isArray()) {
					if (val instanceof List) {
						List list = (List) val;
						Object arr = Array.newInstance(paramType.getComponentType(), list.size());
						for (int i = 0; i < list.size(); i++) {
							Array.set(arr, i, list.get(i));
						}
						return arr;
					} else if (paramType == Resource[].class){
						return ScanUtil.getResources(val.toString());
					}
				} else{
					if(val.toString().contains("ref:")) {
						return LazyBean.findBean(val.toString().replace("ref:", ""));
					}
					log.warn("其他类型 =>{}",paramType);
				}
			}
		} catch (Exception e) {
			log.warn("其他类型");
		}
		return null;
	}

	private static void loadXmlNodeProp2(Map<String, Object> attr, Map<String, LazyBeanInitProcessImpl> tmpAttrMap) {
//		log.info("处理 二级");
		attr.keySet().forEach(str -> {
			Object v = attr.get(str);
			if (v instanceof Node) {
				Node node = (Node) v;
				if (node.getNodeName().equals("array")) {
					attr.put(str, loadXmlNodeArr(node.getChildNodes(),tmpAttrMap));
				} else {
					log.info("位置 数据结构");
				}
			}
		});
	}

	private static void processAttr(String className, Map<String, Object> attr) {
		
	}

	public static List<Object> loadXmlNodeArr(NodeList nodeList, Map<String, LazyBeanInitProcessImpl> tmpAttrMap) {
		List<Object> list = Lists.newArrayList();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node item = nodeList.item(i);
			Element prop = null;
			if (item instanceof Element) {
				prop = (Element) item;
			} else {
				continue;
			}
			if (prop.getNodeName().equals("bean")) {
				list.add(registerBean(prop.getAttribute("class"), prop ,tmpAttrMap));
			}
		}
		return list;
	}

	public static Map<String, Object> loadXmlNodeProp(NodeList list) {
		Map<String, Object> map = Maps.newHashMap();
		one: for (int i = 0; i < list.getLength(); i++) {
			Node item = list.item(i);
			Element prop = null;
			if (item instanceof Element) {
				prop = (Element) item;
			} else {
				continue;
			}
			if (prop.getNodeName().equals("property")) {
				String key = prop.getAttribute("name");
				if (StringUtils.isNotBlank(prop.getAttribute("value"))) {
					map.put(key, prop.getAttribute("value"));
				} else if (StringUtils.isNotBlank(prop.getAttribute("ref"))) {
					String beanName = prop.getAttribute("ref");
					Object obj = LazyBean.findBean(beanName);
					if(obj == null) {
						throw new RuntimeException("xml=>"+beanName+" not find");
					}
					map.put(key, obj);
				} else if (StringUtils.isNotBlank(list.item(i).getNodeValue())) {
					map.put(key, list.item(i).getNodeValue());
				} else if (prop.hasChildNodes()) {
					NodeList nC = list.item(i).getChildNodes();
					List<Node> ll = Lists.newArrayList();
					for (int j = 0; j < nC.getLength(); j++) { // 读取一层关系
						Node tmpN = nC.item(j);
						if (tmpN instanceof Element) {
							String nN = tmpN.getNodeName();
							if (nN.equals("array")) {
								map.put(key, tmpN);
								continue one;
							} else if (nN.equals("map")) {
//								ll.add(tmpN);
								Map<String, Object> tmpMap = Maps.newHashMap();
								NodeList mNL = tmpN.getChildNodes();
								for (int m = 0; m < mNL.getLength(); m++) {
									Node tmpmN = mNL.item(m);
									if (tmpmN instanceof Element) {
										Element mprop = (Element) tmpmN;
										String beanName = mprop.getAttribute("value-ref");
										Object obj = LazyBean.findBean(beanName);
										if(obj == null) {
											throw new RuntimeException("xml=>"+beanName+" not find");
										}
										tmpMap.put(mprop.getAttribute("key"), obj);
									}
								}
								map.put(key, tmpMap);
								continue one;
							} else if (nN.equals("value")) {
								Node value = tmpN.getFirstChild();
								map.put(key, value.getTextContent().trim());
							} else {
								ll.add(tmpN);
							}
						}
					}
				}
			}
		}
		return map;
	}

	public static Element getBeanById(Document document, String id) {
		NodeList list = document.getElementsByTagName("bean");
		for (int i = 0; i < list.getLength(); i++) {
			Element item = (Element) list.item(i);
			if (Objects.equal(id, item.getAttribute("id"))) {
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

	public static List<Node> findNodeByTag(Node node, String tagName) {
		List<Node> list = Lists.newArrayList();
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i).getNodeName().equals(tagName)) {
				list.add(nodeList.item(i));
			} else {
				if (nodeList.item(i).hasChildNodes()) {
					list.addAll(findNodeByTag(nodeList.item(i), tagName));
				}
			}
		}
		return list;
	}
}
