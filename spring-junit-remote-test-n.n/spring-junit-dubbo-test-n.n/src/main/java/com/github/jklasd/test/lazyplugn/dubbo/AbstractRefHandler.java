package com.github.jklasd.test.lazyplugn.dubbo;

import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.dubbo.staticbean.JunitDubboFilterHandler;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRefHandler implements DubboHandler{
	protected static Map<Class<?>,Object> dubboData = Maps.newHashMap();
	
	protected JunitDubboFilterHandler filterHandler = new JunitDubboFilterHandler();
	
	public static void putAnnService(Class<?> dubboServiceClass) {}
	
	public final static Integer TypeXml = 1;
	public final static Integer TypeAnn = 2;
	
	@Getter
	protected Map<String,Integer> refType = Maps.newHashMap();
	
	@Getter
	protected Map<String,BeanDefinition> dubboRefferCacheDef = Maps.newHashMap();
	@Getter
	protected Map<String,BeanDefinition> dubboServiceCacheDef = Maps.newHashMap();
	@Getter
	protected Map<String,BeanDefinition> dubboConfigCacheDef = Maps.newHashMap();
	
	protected LazyListableBeanFactory lazyBeanFactory = LazyListableBeanFactory.getInstance();
	
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
                configCenterConfig = LazyListableBeanFactory.getInstance().doCreateBean("dubboConfigCenterConfig", beanDef, null);
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
                 protocol = LazyListableBeanFactory.getInstance().doCreateBean("dubboProtocolConfig", beanDef, null);
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
                consumer = LazyListableBeanFactory.getInstance().doCreateBean("dubboConsumerConfig", beanDef, null);
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
                providerConfig = LazyListableBeanFactory.getInstance().doCreateBean("dubboProviderConfig", beanDef, null);
            }else {
                // 扫描Configuration
            	providerConfig = scanConfigruation("com.alibaba.dubbo.config.ProviderConfig","org.apache.dubbo.config.ProviderConfig");
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
            	registryCenter = LazyListableBeanFactory.getInstance().doCreateBean("dubboRegistryCenter", beanDef, null);
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
                application = LazyListableBeanFactory.getInstance().doCreateBean("dubboApplication", beanDef, null);
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
