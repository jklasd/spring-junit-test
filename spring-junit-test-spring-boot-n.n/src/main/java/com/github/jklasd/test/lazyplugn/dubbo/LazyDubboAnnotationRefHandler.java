package com.github.jklasd.test.lazyplugn.dubbo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.util.JunitInvokeUtil;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyDubboAnnotationRefHandler extends AbstractRefHandler{
	private static LazyDubboAnnotationRefHandler bean = new LazyDubboAnnotationRefHandler();
	public static LazyDubboAnnotationRefHandler getInstance() {
		return bean;
	}
	
	private Class<?> referenceConfigC = ScanUtil.loadClass("org.apache.dubbo.config.ReferenceConfig");
	public Object buildBeanRef(Class<?> dubboClass, Annotation ann) {
        if(dubboData.containsKey(dubboClass)) {
            return dubboData.get(dubboClass);
        }
        log.info("构建Dubbo 代理服务=>{}",dubboClass);
        try {
            Object referenceConfig = referenceConfigC.newInstance();
            Method[] ms = ann.annotationType().getDeclaredMethods();
            for(Method m : ms) {
                Object pv = m.invoke(ann);
                if(pv!=null) {
                    log.debug("m=>{},v=>{}",m.getName(),pv);
                }
                if(pv instanceof String) {
                    if(pv.toString().length()>0) {
                        LazyBean.getInstance().setAttr(m.getName(), referenceConfig, referenceConfigC, pv);
                    }
                }else if(pv instanceof String[]) {
                    if(((String[])pv).length>0) {
                        LazyBean.getInstance().setAttr(m.getName(), referenceConfig, referenceConfigC, pv);
                    }
                }else if(pv instanceof Integer || pv instanceof Boolean) {
                    LazyBean.getInstance().setAttr(m.getName(), referenceConfig, referenceConfigC, pv);
                }
            }
            LazyBean.getInstance().setAttr("injvm", referenceConfig, referenceConfigC, false);//关闭本地
            LazyBean.getInstance().setAttr("interface", referenceConfig, referenceConfigC, dubboClass.getName());
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

	@Override
	public Object buildBeanNew(Class<?> dubboClass) {
		return null;
	}

	@Override
	public boolean isDubboNew(Class<?> classBean) {
		return false;
	}

	@Override
	public void registerDubboService(Class<?> exportService) {
		
	}
}
