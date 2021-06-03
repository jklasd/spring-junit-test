package com.github.jklasd.test.lazyplugn.dubbo;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.w3c.dom.Element;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.lazybean.beanfactory.AbastractLazyProxy;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.BeanDefParser;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.github.jklasd.test.util.InvokeUtil;
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
		return Service!=null;
	}
	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation> Service = ScanUtil.loadClass("com.alibaba.dubbo.config.annotation.Service");
	@SuppressWarnings("unchecked")
    private static final Class<? extends Annotation> apacheService = ScanUtil.loadClass("org.apache.dubbo.config.annotation.DubboService");
	public static final Class<? extends Annotation> getAnnotionClass() {
	    if(apacheService!=null) {
	        return apacheService;
	    }
		if(Service!=null ) {
			return Service;
		}
		return null;
	}
	
	@SuppressWarnings("unlikely-arg-type")
    public void registerDubboService(Class<?> dubboServiceClass) {
		if(dubboServiceCacheDef.containsKey(dubboServiceClass)) {
			log.info("注册dubboService=>{}",dubboServiceClass);
	        RootBeanDefinition beanDef = (RootBeanDefinition)dubboRefferCacheDef.get(dubboServiceClass.getName());
	        try {
	            Object referenceConfig = beanDef.getBeanClass().newInstance();
	            beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
	                LazyBean.getInstance().setAttr(pv.getName(), referenceConfig, beanDef.getBeanClass(), pv.getValue());
	            });
	            InvokeUtil.invokeMethod(referenceConfig, "setApplication",getApplication());
	            InvokeUtil.invokeMethod(referenceConfig, "setRegistry",getRegistryConfig());
//	            Object obj = 
	                InvokeUtil.invokeMethod(referenceConfig, "export");
	        } catch (Exception e) {
	            log.error("构建Dubbo 代理服务",e);
	        }
			log.info("注册=========={}===============成功",dubboServiceClass);
		}
	}
	
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
        if(beanDef == null) {
            beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("org.apache.dubbo.config.RegistryConfig");  
        }
        Class<?> registerClass = beanDef.getBeanClass();
        registryCenter = beanDef.getBeanClass().newInstance();
        beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
            LazyBean.getInstance().setAttr(pv.getName(), registryCenter, registerClass, pv.getValue());
        });
        return registryCenter;
    }

    private volatile Object application;
    private Object getApplication() throws InstantiationException, IllegalAccessException, IllegalStateException {
        if(application != null) {
            return application;
        }
        RootBeanDefinition beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("com.alibaba.dubbo.config.ApplicationConfig");
        if(beanDef == null) {
            beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("org.apache.dubbo.config.ApplicationConfig");
        }
        application = beanDef.getBeanClass().newInstance();
        PropertyValue name = beanDef.getPropertyValues().getPropertyValue("name");
        LazyBean.getInstance().setAttr("name", application, beanDef.getBeanClass(), TestUtil.getInstance().getPropertiesValue(name.getValue().toString()));
        return application;
    }

    @Override
    public Object buildBean(AbastractLazyProxy model) {
        if (useDubbo()) {
            Class<?> dubboClass = model.getBeanModel().getTagClass();
            return buildBeanNew(dubboClass);
        }
        return null;
    }
}
