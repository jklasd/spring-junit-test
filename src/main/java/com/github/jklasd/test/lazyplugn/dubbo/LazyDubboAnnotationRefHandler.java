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
	
	private static List<Class<? extends Annotation>> referenceList = Lists.newArrayList();
    static {
    	Lists.newArrayList("org.apache.dubbo.config.annotation.Reference","com.alibaba.dubbo.config.annotation.Reference").forEach(className->{
    		@SuppressWarnings("unchecked")
			Class<? extends Annotation> refAnn = ScanUtil.loadClass(className);
    		if(refAnn == null)
    			return;
    		referenceList.add(refAnn);
    	});
    }
	
	public void processAttr(Object tagertObj, Class<?> objClassOrSuper) {
		if(referenceList.isEmpty() || tagertObj == null) return;
		objClassOrSuper.getDeclaredFields();
        Field[] fields = objClassOrSuper.getDeclaredFields();
        processField(tagertObj, fields);
        
        Class<?> superC = objClassOrSuper.getSuperclass();
        if (superC != null) {
            processAttr(tagertObj, superC);
        }
	}
	
	private void processField(Object obj, Field[] fields) {
        for(Field f : fields){
        	
        	for(Class<? extends Annotation> annRefC: referenceList) {
        		Annotation annR = f.getAnnotation(annRefC);
        		if(annR!=null) {
        			Object ref = buildBeanRef(f.getType(),annR);
        			if(ref!=null) {
        				LazyBean.getInstance().setObj(f, obj, ref);
        			}
        			break;
        		}
        	}
        }
    }
	private Class<?> referenceConfigC = ScanUtil.loadClass("org.apache.dubbo.config.ReferenceConfig");
	private Object buildBeanRef(Class<?> dubboClass, Annotation aliR) {
        if(dubboData.containsKey(dubboClass)) {
            return dubboData.get(dubboClass);
        }
        log.info("构建Dubbo 代理服务=>{}",dubboClass);
        try {
            Object referenceConfig = referenceConfigC.newInstance();
            Method[] ms = aliR.annotationType().getDeclaredMethods();
            for(Method m : ms) {
                Object pv = m.invoke(aliR);
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
