package com.github.jklasd.test.lazyplugn.dubbo;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.w3c.dom.Element;

import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.BaseAbstractLazyProxy;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.BeanDefParser;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author jubin.zhang
 *
 */
@Slf4j
public class LazyDubboBean implements BeanDefParser,LazyPlugnBeanFactory{
    private LazyDubboBean() {}
    private static volatile LazyDubboBean bean;
    public static LazyDubboBean getInstance() {
        if(bean == null) {
            bean = new LazyDubboBean();
        }
        return bean;
    }
    
	private static Map<Class<?>,Object> dubboData = Maps.newHashMap();
	
	public static final boolean useDubbo() {
		return aliService!=null||apacheService!=null;
	}
	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation> aliService = ScanUtil.loadClass("com.alibaba.dubbo.config.annotation.Service");
	@SuppressWarnings("unchecked")
    private static final Class<? extends Annotation> apacheService = ScanUtil.loadClass("org.apache.dubbo.config.annotation.DubboService");
	public static final Class<? extends Annotation> getAnnotionClass() {
	    if(apacheService!=null) {
	        return apacheService;
	    }
		if(aliService!=null ) {
			return aliService;
		}
		return null;
	}
	private AbstractRefHandler xmlRefHandler = LazyDubboXmlRefHandler.getInstance();
	private AbstractRefHandler annRefHandler = LazyDubboAnnotationRefHandler.getInstance();
	
	public void handBeanDef(Element ele,BeanDefinition beanDef) {
		String beanName = null;
        switch (ele.getTagName()) {
            case "dubbo:reference":
            	beanName = beanDef.getPropertyValues().getPropertyValue("interface").getValue().toString();
            	xmlRefHandler.refType.put(beanName, xmlRefHandler.TypeXml);
            	xmlRefHandler.dubboRefferCacheDef.put(beanName, beanDef);
                break;
            case "dubbo:service":
            	beanName = beanDef.getPropertyValues().getPropertyValue("interface").getValue().toString();
            	xmlRefHandler.dubboServiceCacheDef.put(beanName, beanDef);
                break;
            default:
            	beanName = beanDef.getBeanClassName();
            	xmlRefHandler.dubboConfigCacheDef.put(beanName, beanDef);
                break;
        }
    }
    public void load(Map<String, BeanDefParser> parser) {
        XmlBeanUtil.getInstance().getNamespaceURIList().stream().filter(url->url.contains("dubbo")).forEach(url->{
            parser.put(url, this);
        });
    }
    public boolean isDubboNew(Class<?> classBean) {
        return xmlRefHandler.isDubboNew(classBean)
        		|| annRefHandler.isDubboNew(classBean);
    }
    
    public Object buildBeanNew(Class<?> dubboClass, String beanName) {
    	if(xmlRefHandler.refType.containsKey(dubboClass.getName())) {
    		return xmlRefHandler.buildBeanNew(dubboClass,beanName);
    	}else if(annRefHandler.refType.containsKey(dubboClass.getName())) {
    		return annRefHandler.buildBeanNew(dubboClass,beanName);
    	}
    	return null;
    }

    @Override
    public Object buildBean(BaseAbstractLazyProxy model) {
        if (useDubbo()) {
            Class<?> dubboClass = model.getBeanModel().getTagClass();
            return buildBeanNew(dubboClass,model.getBeanModel().getBeanName());
        }
        return null;
    }

//    public void processAttr(Object tagertObj, Class<?> objClassOrSuper) {
//    	LazyDubboAnnotationRefHandler.getInstance().processAttr(tagertObj, objClassOrSuper);
//    }
	public void registerDubboService(Class<?> class1) {
		xmlRefHandler.registerDubboService(class1);
	}
    
}
