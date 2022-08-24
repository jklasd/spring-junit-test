package com.github.jklasd.test.lazyplugn.dubbo;

import java.util.Map;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.w3c.dom.Element;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRefHandler implements DubboHandler{
	protected static Map<Class<?>,Object> dubboData = Maps.newHashMap();
	public static void putAnnService(Class<?> dubboServiceClass) {}
	
	protected final static Integer TypeXml = 1;
	protected final static Integer TypeAnn = 2;
	
	protected Map<String,Integer> refType = Maps.newHashMap();
	
	protected Map<String,BeanDefinition> dubboRefferCacheDef = Maps.newHashMap();
	protected Map<String,BeanDefinition> dubboServiceCacheDef = Maps.newHashMap();
	protected Map<String,BeanDefinition> dubboConfigCacheDef = Maps.newHashMap();
	
	private static DubboConfig dubboConfiguration;
	{
		if(dubboConfiguration==null)
		dubboConfiguration = new DubboConfig();
	}
    protected Object getConfigCenterConfig() throws InstantiationException, IllegalAccessException, IllegalStateException {
    	return dubboConfiguration.getConfigCenterConfig();
    }
    protected Object getConsumer() throws InstantiationException, IllegalAccessException, IllegalStateException {
    	return dubboConfiguration.getConsumer();
    }
    protected Object getProviderConfig() throws InstantiationException, IllegalAccessException, IllegalStateException {
    	return dubboConfiguration.getProviderConfig();
    }
    protected Object getRegistryConfig() throws InstantiationException, IllegalAccessException, IllegalStateException {
    	return dubboConfiguration.getRegistryConfig();
    }
    protected Object getApplication() throws InstantiationException, IllegalAccessException, IllegalStateException {
    	return dubboConfiguration.getApplication();
    }
    protected Object getProtocol() throws InstantiationException, IllegalAccessException, IllegalStateException {
    	return dubboConfiguration.getProtocol();
    }
    
    class DubboConfig{
    	private Object configCenterConfig;
        private Object getConfigCenterConfig() throws InstantiationException, IllegalAccessException, IllegalStateException {
            if (configCenterConfig != null) {
                return configCenterConfig;
            }
            RootBeanDefinition beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("org.apache.dubbo.config.spring.ConfigCenterConfig");
            if(beanDef == null) {
                beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("org.apache.dubbo.config.ConfigCenterConfig");  
            }
            if(beanDef!=null) {
                Class<?> registerClass = beanDef.getBeanClass();
                configCenterConfig = beanDef.getBeanClass().newInstance();
                beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
                    LazyBean.getInstance().setAttr(pv.getName(), configCenterConfig, registerClass, XmlBeanUtil.getInstance().conversionValue(pv));
                });
            }else {
                //扫描Configuration
                configCenterConfig = scanConfigruation("com.alibaba.dubbo.config.ConfigCenterConfig","org.apache.dubbo.config.ConfigCenterConfig");
            }
            return configCenterConfig;
        }
		private Object protocol;
        private Object getProtocol() throws InstantiationException, IllegalAccessException, IllegalStateException {
             if(protocol!=null) {
                 return protocol;
             }
             RootBeanDefinition beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("com.alibaba.dubbo.config.ProtocolConfig");
             if(beanDef == null) {
                 beanDef = (RootBeanDefinition)dubboConfigCacheDef.get("org.apache.dubbo.config.ProtocolConfig");  
             }
             if(beanDef!=null) {
                 Class<?> registerClass = beanDef.getBeanClass();
                 protocol = beanDef.getBeanClass().newInstance();
                 beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
                     LazyBean.getInstance().setAttr(pv.getName(), protocol, registerClass, pv.getValue());
                 });
             }else {
                 //扫描Configuration
                 protocol = scanConfigruation("com.alibaba.dubbo.config.ProtocolConfig","org.apache.dubbo.config.ProtocolConfig");
             }
             return protocol;
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

        private synchronized Object getRegistryConfig() throws InstantiationException, IllegalAccessException, IllegalStateException {
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
}
