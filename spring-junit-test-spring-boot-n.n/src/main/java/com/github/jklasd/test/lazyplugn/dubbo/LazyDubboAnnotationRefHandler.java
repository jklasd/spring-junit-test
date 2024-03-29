package com.github.jklasd.test.lazyplugn.dubbo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.util.JunitInvokeUtil;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyDubboAnnotationRefHandler extends AbstractRefHandler{
	private static LazyDubboAnnotationRefHandler bean = new LazyDubboAnnotationRefHandler();
	public static LazyDubboAnnotationRefHandler getInstance() {
		return bean;
	}
	
	private Class<?> referenceConfigC = ScanUtil.loadClass("org.apache.dubbo.config.ReferenceConfig");
	
	public Map<String,Class<?>> cacheClass = Maps.newHashMap();
	public Map<String,Annotation> cacheAnn = Maps.newHashMap();
	
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
                	if(pv instanceof String && StringUtils.isBlank(pv.toString())) {
                		continue;
                	}
                    log.debug("m=>{},v=>{}",m.getName(),pv);
                }
                if(pv instanceof String) {
                	LazyBean.getInstance().setFieldValueFromExpression(m.getName(), referenceConfig, referenceConfigC, pv);
                }else if(pv instanceof String[]) {
                    if(((String[])pv).length>0) {
                        LazyBean.getInstance().setFieldValueFromExpression(m.getName(), referenceConfig, referenceConfigC, pv);
                    }
                }else if(pv instanceof Integer || pv instanceof Boolean) {
                    LazyBean.getInstance().setFieldValueFromExpression(m.getName(), referenceConfig, referenceConfigC, pv);
                }
            }
            LazyBean.getInstance().setFieldValueFromExpression("injvm", referenceConfig, referenceConfigC, false);//关闭本地
            LazyBean.getInstance().setFieldValueFromExpression("interface", referenceConfig, referenceConfigC, dubboClass.getName());
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
	public Object buildBeanNew(Class<?> dubboClass,String beanName) {
		return buildBeanRef(dubboClass, cacheAnn.get(beanName));
	}

	@Override
	public boolean isDubboNew(Class<?> classBean) {
		return refType.containsKey(classBean.getName());
	}

	@Override
	public void registerDubboService(Class<?> exportService) {
		
	}

	public void registerBeanDef(Field field, Annotation ann) {
		String beanName = field.getName();
		Class<?> type = field.getType();
		refType.put(type.getName(), TypeAnn);
		cacheClass.put(beanName, type);
		cacheAnn.put(beanName, ann);
	}
}
