package com.github.jklasd.test.lazyplugn.dubbo;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationEventPublisher;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.github.jklasd.test.util.JunitInvokeUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyDubboXmlRefHandler extends AbstractRefHandler{
	private static LazyDubboXmlRefHandler bean = new LazyDubboXmlRefHandler();
	private LazyDubboXmlRefHandler() {}
	public static LazyDubboXmlRefHandler getInstance() {
		return bean;
	}
	
	@Override
	public Object buildBeanNew(Class<?> dubboClass,String beanName) {
		if(dubboData.containsKey(dubboClass)) {
            return dubboData.get(dubboClass);
        }
        log.debug("构建Dubbo 代理服务=>{}",dubboClass);
        
        
        RootBeanDefinition beanDef = (RootBeanDefinition)dubboRefferCacheDef.get(dubboClass.getName());
        try {
            Object referenceConfig = lazyBeanFactory.onlyCreateBean(beanName, beanDef, null);
            /**
             * 待后续升级，可能存在动态设定协议或者客户端问题
             */
//            lazyBeanFactory.getBean(null);
            JunitInvokeUtil.invokeMethod(referenceConfig, "setConsumer",getConsumer());
            JunitInvokeUtil.invokeMethod(referenceConfig, "setApplication",getApplication());
            JunitInvokeUtil.invokeMethod(referenceConfig, "setRegistry",getRegistryConfig());
            if(getConfigCenterConfig()!=null) {
                JunitInvokeUtil.invokeMethodByParamClass(referenceConfig, "setConfigCenter",new Class[] {ScanUtil.loadClass("org.apache.dubbo.config.ConfigCenterConfig")},new Object[] {getConfigCenterConfig()});
            }
            FactoryBean<?> fb = (FactoryBean<?>) referenceConfig;
            Object obj = fb.getObject();
            dubboData.put(dubboClass,obj);
            filterHandler.exec(obj);
            return obj;
        } catch (Exception e) {
            log.error("构建Dubbo 代理服务",e);
            return null;
        }
	}
	
	public void registerDubboService(Class<?> dubboServiceClass) {
		if(dubboServiceCacheDef.containsKey(dubboServiceClass.getName())) {
			log.debug("注册dubboService=>{}",dubboServiceClass);
	        RootBeanDefinition beanDef = (RootBeanDefinition)dubboServiceCacheDef.get(dubboServiceClass.getName());
	        try {
	        	//TODO 待处理
	            Object serviceConfig = LazyListableBeanFactory.getInstance().doCreateBean(dubboServiceClass.getName(), beanDef, null);
	            
	            JunitInvokeUtil.invokeMethod(serviceConfig, "setApplication",getApplication());
	            JunitInvokeUtil.invokeMethod(serviceConfig, "setRegistry",getRegistryConfig());
	            JunitInvokeUtil.invokeMethod(serviceConfig, "setProvider",getProviderConfig());
	            JunitInvokeUtil.invokeMethod(serviceConfig, "setProtocol",getProtocol());
	            JunitInvokeUtil.invokeMethodByParamClass(serviceConfig, "setApplicationEventPublisher",new Class[] {ApplicationEventPublisher.class},
	                new Object[] {TestUtil.getInstance().getApplicationContext()});
	            if(getConfigCenterConfig()!=null) {
	                JunitInvokeUtil.invokeMethodByParamClass(serviceConfig, "setConfigCenter",new Class[] {ScanUtil.loadClass("org.apache.dubbo.config.ConfigCenterConfig")},new Object[] {getConfigCenterConfig()});
	            }
	            
                JunitInvokeUtil.invokeMethod(serviceConfig, "export");
                log.debug("注册=========={}===============成功",dubboServiceClass);
	        } catch (Exception e) {
	            log.error("构建Dubbo 代理服务",e);
	        }
		}
	}
	
	@Override
	public boolean isDubboNew(Class<?> classBean) {
		return dubboRefferCacheDef.containsKey(classBean.getName());
	}
}
