package com.github.jklasd.test.lazyplugn.dubbo;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Element;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.lazybean.beanfactory.AbastractLazyProxy;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.BeanDefParser;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.github.jklasd.test.util.JunitInvokeUtil;
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
	
	public void registerDubboService(Class<?> dubboServiceClass) {
		if(dubboServiceCacheDef.containsKey(dubboServiceClass.getName())) {
			log.info("注册dubboService=>{}",dubboServiceClass);
	        RootBeanDefinition beanDef = (RootBeanDefinition)dubboServiceCacheDef.get(dubboServiceClass.getName());
	        try {
	            Object serviceConfig = beanDef.getBeanClass().newInstance();
	            beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
	                if(pv.getName().equals("ref")) {
	                    log.info("断点");
	                    Object instance = beanDef.getPropertyValues().get("interface");
	                    Object value = XmlBeanUtil.getInstance().conversionValue(pv);
	                    if(value == null) {
	                        RuntimeBeanReference tmp = (RuntimeBeanReference)pv.getValue();
	                        value = LazyBean.getInstance().buildProxy(ScanUtil.loadClass(instance.toString()), tmp.getBeanName());
	                    }
	                    LazyBean.getInstance().setAttr(pv.getName(), serviceConfig, beanDef.getBeanClass(), value);
	                }else {
	                    LazyBean.getInstance().setAttr(pv.getName(), serviceConfig, beanDef.getBeanClass(), XmlBeanUtil.getInstance().conversionValue(pv));
	                }
	            });
	            
	            JunitInvokeUtil.invokeMethod(serviceConfig, "setApplication",getApplication());
	            JunitInvokeUtil.invokeMethod(serviceConfig, "setRegistry",getRegistryConfig());
	            JunitInvokeUtil.invokeMethod(serviceConfig, "setProvider",getProviderConfig());
	            JunitInvokeUtil.invokeMethodByParamClass(serviceConfig, "setApplicationEventPublisher",new Class[] {ApplicationEventPublisher.class},
	                new Object[] {TestUtil.getInstance().getApplicationContext()});
//	            Object obj = 
	                JunitInvokeUtil.invokeMethod(serviceConfig, "export");
	                log.info("注册=========={}===============成功",dubboServiceClass);
	        } catch (Exception e) {
	            log.error("构建Dubbo 代理服务",e);
	        }
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
                dubboServiceCacheDef.put(beanDef.getPropertyValues().getPropertyValue("interface").getValue().toString(), beanDef);
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
                LazyBean.getInstance().setAttr(pv.getName(), referenceConfig, beanDef.getBeanClass(), XmlBeanUtil.getInstance().conversionValue(pv));
            });
            /**
             * 待后续升级，可能存在动态设定协议或者客户端问题
             */
            JunitInvokeUtil.invokeMethod(referenceConfig, "setConsumer",getConsumer());
            JunitInvokeUtil.invokeMethod(referenceConfig, "setApplication",getApplication());
            JunitInvokeUtil.invokeMethod(referenceConfig, "setRegistry",getRegistryConfig());
            if(getConfigCenterConfig()!=null) {
                JunitInvokeUtil.invokeMethodByParamClass(referenceConfig, "setConfigCenter",new Class[] {ScanUtil.loadClass("org.apache.dubbo.config.ConfigCenterConfig")},new Object[] {getConfigCenterConfig()});
            }
            Object obj = JunitInvokeUtil.invokeMethod(referenceConfig, "get");
            dubboData.put(dubboClass,obj);
            return obj;
        } catch (Exception e) {
            log.error("构建Dubbo 代理服务",e);
            return null;
        }
    }
    private Object configCenterConfig;
    private Object getConfigCenterConfig() throws InstantiationException, IllegalAccessException, IllegalStateException {
        if (configCenterConfig != null) {
            return configCenterConfig;
        }
        RootBeanDefinition beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("org.apache.dubbo.config.spring.ConfigCenterBean");
        if(beanDef == null) {
            beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("org.apache.dubbo.config.ConfigCenterBean");  
        }
        if(beanDef!=null) {
            Class<?> registerClass = beanDef.getBeanClass();
            configCenterConfig = beanDef.getBeanClass().newInstance();
            beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
                LazyBean.getInstance().setAttr(pv.getName(), configCenterConfig, registerClass, XmlBeanUtil.getInstance().conversionValue(pv));
            });
        }else {
            //扫描Configuration
            configCenterConfig = scanConfigruation("com.alibaba.dubbo.config.ConfigCenterBean","org.apache.dubbo.config.ConfigCenterBean");
        }
        return configCenterConfig;
    }

    private void getProtocol() {
         
    }
    private Object consumer;
    private Object getConsumer() throws InstantiationException, IllegalAccessException, IllegalStateException {
        if (consumer != null) {
            return consumer;
        }
        RootBeanDefinition beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("com.alibaba.dubbo.config.ConsumerConfig");
        if(beanDef == null) {
            beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("org.apache.dubbo.config.ConsumerConfig");  
        }
        if(beanDef!=null) {
            Class<?> registerClass = beanDef.getBeanClass();
            consumer = beanDef.getBeanClass().newInstance();
            beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
                LazyBean.getInstance().setAttr(pv.getName(), consumer, registerClass, pv.getValue());
            });
        }else {
            //扫描Configuration
            consumer = scanConfigruation("com.alibaba.dubbo.config.ConsumerConfig","org.apache.dubbo.config.ConsumerConfig");
        }
        return consumer;
    }
    private Object providerConfig;
    private Object getProviderConfig() throws InstantiationException, IllegalAccessException, IllegalStateException {
        if (providerConfig != null) {
            return providerConfig;
        }
        RootBeanDefinition beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("com.alibaba.dubbo.config.ProviderConfig");
        if(beanDef == null) {
            beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("org.apache.dubbo.config.ProviderConfig");  
        }
        if(beanDef!=null) {
            Class<?> registerClass = beanDef.getBeanClass();
            registryCenter = beanDef.getBeanClass().newInstance();
            beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
                LazyBean.getInstance().setAttr(pv.getName(), registryCenter, registerClass, pv.getValue());
            });
        }else {
            // 扫描Configuration
            registryCenter = scanConfigruation("com.alibaba.dubbo.config.ProviderConfig","org.apache.dubbo.config.ProviderConfig");
        }
        return registryCenter;
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
        if(beanDef!=null) {
            Class<?> registerClass = beanDef.getBeanClass();
            registryCenter = beanDef.getBeanClass().newInstance();
            beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
                LazyBean.getInstance().setAttr(pv.getName(), registryCenter, registerClass, pv.getValue());
            });
        }else {
            // 扫描Configuration
            registryCenter = scanConfigruation("com.alibaba.dubbo.config.RegistryConfig","org.apache.dubbo.config.RegistryConfig");
        }
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
        if(beanDef!=null) {
            application = beanDef.getBeanClass().newInstance();
            PropertyValue name = beanDef.getPropertyValues().getPropertyValue("name");
            LazyBean.getInstance().setAttr("name", application, beanDef.getBeanClass(), TestUtil.getInstance().getPropertiesValue(name.getValue().toString()));
        }else {
         // 扫描Configuration
            application = scanConfigruation("com.alibaba.dubbo.config.ApplicationConfig","org.apache.dubbo.config.ApplicationConfig");
        }
        return application;
    }

    private Object scanConfigruation(String oldC,String newC) {
        Class<?> tmpC = ScanUtil.loadClass(oldC);
        Object obj = null;
        if(tmpC!=null) {
            obj = LazyBean.findCreateBeanFromFactory(tmpC, null);
        }
        if(obj ==null) {
            tmpC = ScanUtil.loadClass(newC);
            if(tmpC!=null) {
                obj = LazyBean.findCreateBeanFromFactory(tmpC, null);
            }
        }
        return obj;
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
