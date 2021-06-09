package com.github.jklasd.test.lazyplugn.spring.xml;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedArray;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.DefaultDocumentLoader;
import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.springframework.beans.factory.xml.DocumentLoader;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.filter.LazyBeanFilter.LazyBeanInitProcessImpl;
import com.github.jklasd.test.lazybean.model.BeanModel;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.github.jklasd.test.util.InvokeUtil;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang
 * 
 */
@Slf4j
public class XmlBeanUtil {
    private XmlBeanUtil() {}
    private DocumentLoader documentLoader = new DefaultDocumentLoader();
    private XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(new LazyListableBeanFactory());
    
	public static List<String> xmlPathList = Lists.newArrayList();

	public synchronized void loadXmlPath(String... xmlPath) {
		for (String path : xmlPath) {
			xmlPathList.add(path);
		}
	}
	
	public void process() {
		xmlPathList.forEach(xml -> readNode(xml));
	}
	/**
	 * 转换类型
	 * @param attr 赋值源数据
	 * @param tabClass 目标类
	 */
	public void processValue(Map<String, Object> attr, Class<?> tabClass) {
		Map<String,Boolean> finded = Maps.newHashMap();
		Map<String,Boolean> sameType = Maps.newHashMap();
		attr.keySet().forEach(field -> {
			Object val = attr.get(field);
			if(val == null) 
			    return;
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
    public Object processTypeValue(String field, Object val, Class<?> paramType) {
		try {
			if (ScanUtil.isImple(paramType, Map.class)) {
				Object prop = paramType.newInstance();
				if(val.toString().contains("=")) {
					String[] k_v = val.toString().split("=");
					((Map<String, String>) prop).put(k_v[0], k_v[1]);
					return prop;
				}
			}else if (paramType == Map.class) {
				Object prop = Maps.newHashMap();
				String[] k_v = val.toString().split("=");
				((Map<String, String>) prop).put(k_v[0], k_v[1]);
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
//					if(val.toString().contains("ref:")) {
//						return LazyBean.getInstance().findBean(val.toString().replace("ref:", ""));
//					}
					log.warn("其他类型 =>{}",paramType);
				}
			}
		} catch (Exception e) {
			log.warn("其他类型");
		}
		return val;
	}

	private static XmlBeanUtil bean;
    public static XmlBeanUtil getInstance() {
        if(bean == null) {
            bean = new XmlBeanUtil();
        }
         return bean;
    }
    
    protected final Log logger = LogFactory.getLog(getClass());
    private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

    public void readNode(String xml) {
        Resource file = TestUtil.getInstance().getApplicationContext().getResource(xml);
        if (file != null) {
            try {
                log.info("load:{}",file.getFile().getPath());
                XmlReaderContext context = xmlReader.createReaderContext(file);
                LazyBeanDefinitionDocumentReader parsor = new LazyBeanDefinitionDocumentReader();
                Document document = documentLoader.loadDocument(new InputSource(file.getInputStream()), getEntityResolver(xmlReader), 
                    errorHandler,(int)InvokeUtil.invokeMethodByParamClass(xmlReader, "getValidationModeForResource",new Class[] {Resource.class}, new Object[] {file}),xmlReader.isNamespaceAware());
                
                parsor.registerBeanDefinitions(document,context);
            } catch (Exception e) {
                log.error("加载xml", e);
            }
        }
    }
    
    private Map<String, LazyBeanInitProcessImpl> tmpAttrMap = Maps.newConcurrentMap();
    public LazyBeanInitProcessImpl getProcess(String key) {
        return tmpAttrMap.get(key);
    }
    
    protected EntityResolver getEntityResolver(XmlBeanDefinitionReader reader) {
        EntityResolver entityResolver = null;
        ResourceLoader resourceLoader = reader.getResourceLoader();
        if (resourceLoader != null) {
            entityResolver = new ResourceEntityResolver(resourceLoader);
        } else {
            entityResolver = new DelegatingEntityResolver(reader.getBeanClassLoader());
        }
        return entityResolver;
    }
    
    private static Map<String, Class<?>> xmlParsors = Maps.newConcurrentMap();
    public Set<String> getNamespaceURIList(){
        return xmlParsors.keySet();
    }
    public void putNameSpace(String mapStr, Class<?> NamespaceHandlerC) {
        if (!xmlParsors.containsKey(mapStr)) {
            xmlParsors.put(mapStr, NamespaceHandlerC);
        }
    }
    
    public void handAttr(Map<String, MutablePropertyValues> attrs) {
        /**
         * 处理bean attr
         */
        attrs.keySet().forEach(key->{
            Class<?> tagC = ScanUtil.loadClass(key.split("-")[0]);
            Map<String, Object> attrParam = handPropValue(attrs.get(key).getPropertyValueList(), tagC);
            XmlBeanUtil.getInstance().processValue(attrParam, tagC);
            XmlBeanUtil.getInstance().getProcess(key).getProcess().init(attrParam);
        });
    }
    public Map<String, Object> handPropValue(List<PropertyValue>  attrs, Class<?> tagC) {
        Map<String, Object> attrParam = Maps.newHashMap();
        attrs.forEach(prov->{
            Object value = conversionValue(prov);
            attrParam.put(prov.getName(), value);
        });
        return attrParam;
    }

    public Object conversionValue(PropertyValue prov) {
        Object value;
        if(prov.getValue() instanceof RuntimeBeanReference) {
            RuntimeBeanReference tmp = (RuntimeBeanReference)prov.getValue();
            value = TestUtil.getInstance().getApplicationContext().getBean(tmp.getBeanName());
        }else if(prov.getValue() instanceof ManagedList) {
            ManagedList<?> tmp = (ManagedList<?>)prov.getValue();
            List<Object> list = Lists.newArrayList();
            tmp.stream().forEach(item ->{
                if(item instanceof BeanDefinitionHolder) {
                    BeanDefinitionHolder tmpBdh = (BeanDefinitionHolder)item;
                    BeanDefinition tmpBd = tmpBdh.getBeanDefinition();
                    Class<?> tmpC= ScanUtil.loadClass(tmpBd.getBeanClassName());
//                        XmlBeanUtil.getInstance().addClass(tmpC);
                    BeanModel beanModel = new BeanModel();
                    beanModel.setTagClass(tmpC);
                    beanModel.setXmlBean(true);
                    beanModel.setPropValue(tmpBd.getPropertyValues());
                    list.add(LazyBean.getInstance().buildProxy(beanModel));
                }else {
                    log.info("ManagedArray=>{}",item);
                }
            });
            value = list;
        }else if(prov.getValue() instanceof ManagedMap) {
            @SuppressWarnings("unchecked")
            ManagedMap<Object, Object> tmp = (ManagedMap<Object, Object>)prov.getValue();
            Map<Object,Object> tmpMap = Maps.newHashMap();
            tmp.forEach((k,v)->{
                if(k instanceof TypedStringValue) {
                    TypedStringValue tmpV = (TypedStringValue)k;
                    k = tmpV.getValue();
                }
                if(v instanceof RuntimeBeanReference) {
                    RuntimeBeanReference tmpV = (RuntimeBeanReference)v;
                    v = TestUtil.getInstance().getApplicationContext().getBean(tmpV.getBeanName());
                }
                tmpMap.put(k, v);
            });
            value = tmpMap;
        }else if(prov.getValue() instanceof TypedStringValue) {
            TypedStringValue tmp = (TypedStringValue)prov.getValue();
            value = tmp.getValue();
        }else {
            log.info("value other=>{}",prov.getValue());
            value = prov.getValue();
        }
        return value;
    }
}
