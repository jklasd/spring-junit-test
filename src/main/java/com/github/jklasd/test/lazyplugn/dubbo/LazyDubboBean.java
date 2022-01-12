package com.github.jklasd.test.lazyplugn.dubbo;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.w3c.dom.Element;

import com.github.jklasd.test.lazybean.beanfactory.AbastractLazyProxy;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.BeanDefParser;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.github.jklasd.test.util.ScanUtil;
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
	private AbstractRefHandler xmlRefHandler = new LazyDubboXmlRefHandler();
	
	public void handBeanDef(Element ele,BeanDefinition beanDef) {
		xmlRefHandler.handBeanDef(ele, beanDef);
    }
    public void load(Map<String, BeanDefParser> parser) {
        XmlBeanUtil.getInstance().getNamespaceURIList().stream().filter(url->url.contains("dubbo")).forEach(url->{
            parser.put(url, this);
        });
    }
    public boolean isDubboNew(Class<?> classBean) {
        return xmlRefHandler.isDubboNew(classBean);
    }
    
    public Object buildBeanNew(Class<?> dubboClass) {
        return xmlRefHandler.buildBeanNew(dubboClass);
    }

    @Override
    public Object buildBean(AbastractLazyProxy model) {
        if (useDubbo()) {
            Class<?> dubboClass = model.getBeanModel().getTagClass();
            return buildBeanNew(dubboClass);
        }
        return null;
    }

    public void processAttr(Object tagertObj, Class<?> objClassOrSuper) {
    	LazyDubboAnnotationRefHandler.getInstance().processAttr(tagertObj, objClassOrSuper);
    }
	public void registerDubboService(Class<?> class1) {
		xmlRefHandler.registerDubboService(class1);
	}
    
}
