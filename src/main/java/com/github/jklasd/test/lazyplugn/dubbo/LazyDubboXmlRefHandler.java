package com.github.jklasd.test.lazyplugn.dubbo;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationEventPublisher;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.github.jklasd.test.util.InvokeUtil;
import com.github.jklasd.test.util.ScanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyDubboXmlRefHandler extends AbstractRefHandler{
	private static LazyDubboXmlRefHandler bean = new LazyDubboXmlRefHandler();
	public static LazyDubboXmlRefHandler getInstance() {
		return bean;
	}
	@Override
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
            InvokeUtil.invokeMethod(referenceConfig, "setConsumer",getConsumer());
            InvokeUtil.invokeMethod(referenceConfig, "setApplication",getApplication());
            InvokeUtil.invokeMethod(referenceConfig, "setRegistry",getRegistryConfig());
            if(getConfigCenterConfig()!=null) {
                InvokeUtil.invokeMethodByParamClass(referenceConfig, "setConfigCenter",new Class[] {ScanUtil.loadClass("org.apache.dubbo.config.ConfigCenterConfig")},new Object[] {getConfigCenterConfig()});
            }
            Object obj = InvokeUtil.invokeMethod(referenceConfig, "get");
            dubboData.put(dubboClass,obj);
            return obj;
        } catch (Exception e) {
            log.error("构建Dubbo 代理服务",e);
            return null;
        }
	}
	
	public void registerDubboService(Class<?> dubboServiceClass) {
		if(dubboServiceCacheDef.containsKey(dubboServiceClass.getName())) {
			log.info("注册dubboService=>{}",dubboServiceClass);
	        RootBeanDefinition beanDef = (RootBeanDefinition)dubboServiceCacheDef.get(dubboServiceClass.getName());
	        try {
	            Object serviceConfig = beanDef.getBeanClass().newInstance();
	            beanDef.getPropertyValues().getPropertyValueList().forEach(pv->{
	                if(pv.getName().equals("ref")) {
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
	            
	            InvokeUtil.invokeMethod(serviceConfig, "setApplication",getApplication());
	            InvokeUtil.invokeMethod(serviceConfig, "setRegistry",getRegistryConfig());
	            InvokeUtil.invokeMethod(serviceConfig, "setProvider",getProviderConfig());
	            InvokeUtil.invokeMethod(serviceConfig, "setProtocol",getProtocol());
	            InvokeUtil.invokeMethodByParamClass(serviceConfig, "setApplicationEventPublisher",new Class[] {ApplicationEventPublisher.class},
	                new Object[] {TestUtil.getInstance().getApplicationContext()});
	            if(getConfigCenterConfig()!=null) {
	                InvokeUtil.invokeMethodByParamClass(serviceConfig, "setConfigCenter",new Class[] {ScanUtil.loadClass("org.apache.dubbo.config.ConfigCenterConfig")},new Object[] {getConfigCenterConfig()});
	            }
//	            Object obj = 
	                InvokeUtil.invokeMethod(serviceConfig, "export");
	                log.info("注册=========={}===============成功",dubboServiceClass);
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
