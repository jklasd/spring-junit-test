package com.github.jklasd.test.dubbo;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.w3c.dom.Element;

import com.github.jklasd.test.InvokeUtil;
import com.github.jklasd.test.LazyBeanFactory;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.beanfactory.LazyBean;
import com.github.jklasd.test.beanfactory.LazyProxy;
import com.github.jklasd.test.spring.BeanDefParser;
import com.github.jklasd.test.spring.xml.XmlBeanUtil;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author jubin.zhang
 *
 */
@Slf4j
public class LazyDubboBean implements BeanDefParser,LazyBeanFactory{
    private LazyDubboBean() {}
    private static volatile LazyDubboBean bean;
    public static LazyDubboBean getInstance() {
        if(bean == null) {
            bean = new LazyDubboBean();
        }
        return bean;
    }
    
	@SuppressWarnings("rawtypes")
	private static Map<Class,Object> dubboData = Maps.newHashMap();
	@SuppressWarnings("rawtypes")
	private static Map<Class,Element> dubboRefferCache = Maps.newHashMap();
	private static Map<Class,Element> dubboServiceCache = Maps.newHashMap();
	
	public static final boolean useDubbo() {
		return Service!=null;
	}
	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation> Service = ScanUtil.loadClass("com.alibaba.dubbo.config.annotation.Service");
	public static final Class<? extends Annotation> getAnnotionClass() {
		if(Service!=null) {
			return Service;
		}
		return null;
	}
	
//	public static boolean isDubbo(Class<?> classBean) {
//		return dubboRefferCache.containsKey(classBean);
//	}
//	private static RegistryConfig registryConfig;
//	public static Object buildBean(Class<?> dubboClass) {
//		if(dubboData.containsKey(dubboClass)) {
//			return dubboData.get(dubboClass);
//		}
//		log.info("构建Dubbo 代理服务=>{}",dubboClass);
//		ReferenceConfig<?> referenceConfig = new ReferenceConfig<>();
//		referenceConfig.setInterface(dubboClass);
//		if(dubboRefferCache.get(dubboClass).hasAttribute("group")) {
//			referenceConfig.setGroup(TestUtil.getPropertiesValue(dubboRefferCache.get(dubboClass).getAttribute("group")));
//		}
//		if(dubboRefferCache.get(dubboClass).hasAttribute("timeout")) {
//			referenceConfig.setTimeout(Integer.valueOf(TestUtil.getPropertiesValue(dubboRefferCache.get(dubboClass).getAttribute("timeout"),dubboRefferCache.get(dubboClass).getAttribute("timeout"))));
//		}else {
//			referenceConfig.setTimeout(10*1000);
//		}
//		ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-examples-consumer");
//		referenceConfig.setApplication(applicationConfig);
//		referenceConfig.setRegistry(registryConfig);
//		Object obj = referenceConfig.get();
//		dubboData.put(dubboClass,obj);
//		return obj;
//	}
//	public static void processDubbo(Document document) {
//		processRegister(document.getElementsByTagName("dubbo:registry"));
//		cacheReference(document.getElementsByTagName("dubbo:reference"));
//		cacheService(document.getElementsByTagName("dubbo:service"));
//		//dubbo:protocol 待处理
//	}
	
	public void registerDubboService(Class<?> dubboServiceClass) {
		if(dubboServiceCacheDef.containsKey(dubboServiceClass)) {
			log.info("注册dubboService=>{}",dubboServiceClass);
			BeanDefinition ele = dubboServiceCacheDef.get(dubboServiceClass);
	        RootBeanDefinition beanDef = (RootBeanDefinition)dubboRefferCacheDef.get(dubboServiceClass.getName());
	        try {
	            Object referenceConfig = beanDef.getBeanClass().newInstance();
	            beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
	                LazyBean.getInstance().setAttr(pv.getName(), referenceConfig, beanDef.getBeanClass(), pv.getValue());
	            });
	            InvokeUtil.invokeMethod(referenceConfig, "setApplication",getApplication());
	            InvokeUtil.invokeMethod(referenceConfig, "setRegistry",getRegistryConfig());
	            Object obj = InvokeUtil.invokeMethod(referenceConfig, "export");
	        } catch (Exception e) {
	            log.error("构建Dubbo 代理服务",e);
	        }
			log.info("注册=========={}===============成功",dubboServiceClass);
		}
	}
//	private static void cacheService(NodeList serviceList) {
//		for(int i = 0 ;i< serviceList.getLength();i++) {
//			Element node = (Element) serviceList.item(i);
//			String className = node.getAttribute("interface");
//			try {
//				dubboServiceCache.put(Class.forName(className),node);
//			} catch (ClassNotFoundException e) {
//				log.error("LazyDubboBean#cacheService=>{}",e.getMessage());
//			}
//		}
//	}
//	private static void cacheReference(NodeList list) {
//			for(int i = 0 ;i< list.getLength();i++) {
//				Element node = (Element) list.item(i);
//				String className = node.getAttribute("interface");
//				try {
//					dubboRefferCache.put(Class.forName(className), node);
//				} catch (Exception e) {
//					log.error("",e);
//				}
//			}
//	}
	
	public static void putAnnService(Class<?> dubboServiceClass) {}
	private Map<String,BeanDefinition> dubboRefferCacheDef = Maps.newHashMap();
	private Map<String,BeanDefinition> dubboServiceCacheDef = Maps.newHashMap();
	private Map<String,BeanDefinition> dubboConfigCacheDef = Maps.newHashMap();
	public void handBeanDef(Element ele,BeanDefinition beanDef) {
        switch (ele.getTagName()) {
            case "dubbo:reference":
                dubboRefferCacheDef.put(beanDef.getPropertyValues().getPropertyValue("interface").getValue().toString(), beanDef);
                break;
            case "dubbo:service":
                dubboServiceCacheDef.put(beanDef.getBeanClassName(), beanDef);
                break;
            default:
                dubboConfigCacheDef.put(beanDef.getBeanClassName(), beanDef);
                break;
        }
    }
    public void load(Map<String, BeanDefParser> parser) {
        XmlBeanUtil.getInstance().getNamespaceURIList().stream().filter(url->url.contains("dubbo")).forEach(url->{
            parser.put(url, this);
        });
    }
    public boolean isDubboNew(Class<?> classBean) {
        return dubboRefferCacheDef.containsKey(classBean.getName());
    }
    public Object buildBeanNew(Class<?> dubboClass) {
        if(dubboData.containsKey(dubboClass)) {
            return dubboData.get(dubboClass);
        }
        log.info("构建Dubbo 代理服务=>{}",dubboClass);
        RootBeanDefinition beanDef = (RootBeanDefinition)dubboRefferCacheDef.get(dubboClass.getName());
        try {
            Object referenceConfig = beanDef.getBeanClass().newInstance();
            beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
                LazyBean.getInstance().setAttr(pv.getName(), referenceConfig, beanDef.getBeanClass(), pv.getValue());
            });
            InvokeUtil.invokeMethod(referenceConfig, "setApplication",getApplication());
            InvokeUtil.invokeMethod(referenceConfig, "setRegistry",getRegistryConfig());
            Object obj = InvokeUtil.invokeMethod(referenceConfig, "get");
            dubboData.put(dubboClass,obj);
            return obj;
        } catch (Exception e) {
            log.error("构建Dubbo 代理服务",e);
            return null;
        }
    }

    private Object registryCenter;

    private Object getRegistryConfig() throws InstantiationException, IllegalAccessException, IllegalStateException {
        if (registryCenter != null) {
            return registryCenter;
        }
        RootBeanDefinition beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("com.alibaba.dubbo.config.RegistryConfig");
        registryCenter = beanDef.getBeanClass().newInstance();
        beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
            LazyBean.getInstance().setAttr(pv.getName(), registryCenter, beanDef.getBeanClass(), pv.getValue());
        });
        return registryCenter;
    }

    private volatile Object application;
    private Object getApplication() throws InstantiationException, IllegalAccessException, IllegalStateException {
        if(application != null) {
            return application;
        }
        RootBeanDefinition beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("com.alibaba.dubbo.config.ApplicationConfig");
        application = beanDef.getBeanClass().newInstance();
        PropertyValue name = beanDef.getPropertyValues().getPropertyValue("name");
        LazyBean.getInstance().setAttr("name", application, beanDef.getBeanClass(), TestUtil.getInstance().getPropertiesValue(name.getValue().toString()));
        return application;
    }

    @Override
    public Object buildBean(LazyProxy model) {
        if (useDubbo()) {
            Class<?> dubboClass = model.getBeanModel().getTagClass();
            return buildBeanNew(dubboClass);
        }
        return null;
    }
}
