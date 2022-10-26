 package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.InitializingBean;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.interf.handler.MockFieldHandlerI;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.github.jklasd.test.util.JunitInvokeUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class AbstractLazyProxy extends BaseAbstractLazyProxy{
    
    protected Object tagertObj;
    protected volatile boolean inited;
    protected LazyApplicationContext applicationContext = LazyApplicationContext.getInstance();
    
    
    public AbstractLazyProxy(BeanModel beanModel) {
        this.beanModel = beanModel;
        if(beanModel.getPropValue()!=null && beanModel.getPropValue().getPropertyValueList().size()>0) {
            attr = XmlBeanUtil.getInstance().handPropValue(beanModel.getPropValue().getPropertyValueList(), beanModel.getTagClass());
            XmlBeanUtil.getInstance().processValue(attr, beanModel.getTagClass());
            beanModel.setPropValue(null);
        }
    }
    
    private MockFieldHandlerI handler = ContainerManager.getComponent(ContainerManager.NameConstants.MockFieldHandler);
    
    private AtomicInteger errorTimes = new AtomicInteger();
    
    protected  Object commonIntercept(Object poxy, Method method, Object[] param) throws Throwable {
    	if(errorTimes.get()>6) {
    		log.error("Class=>{},method=>{}",tagertObj.getClass(),method.getName());
    		throw new JunitException("----------异常代理方式--------",true);
    	}
    	switch (method.getName()) {
		case "toString":
			if(tagertObj!=null) {
				return tagertObj.toString();
			}else {
				return this.toString();
			}
		case "hashCode":
			if(tagertObj!=null) {
    			return tagertObj.hashCode();
    		}else {
    			return this.hashCode();
    		}
		default:
			break;
		}
    	log.debug("exec stack=>{}",method);
		try {
			if (!Modifier.isPublic(method.getModifiers())) {
                // log.warn("非公共方法 class:{},method:{}",tag,method.getName());
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
            }
			
			
			Object result = null;
			if(handler!= null && handler.finded(beanModel)) {
				//	injeckMock拦截
				result = handler.invoke(poxy, method, param,beanModel);
			}else {
				result = LazyProxyManager.getProxyInvoker().invoke(poxy, method, param, beanModel, getTagertObj());
			}
			
			errorTimes.set(0);
			
			return result;
        }catch (JunitException e) {
        	errorTimes.incrementAndGet();
        	throw e;
        }catch (InvocationTargetException e) {
        	errorTimes.incrementAndGet();
        	throw e.getTargetException();
		}catch (Exception e) {
        	errorTimes.incrementAndGet();
            throw e;
        }
    }
    
	protected Object getTagertObj() {
        if (tagertObj != null && inited) {//需初始化完成，才返回，否则存在线程安全问题
            if (tagertObj.getClass().getSimpleName().contains("com.sun.proxy")) {
                log.warn("循环处理代理Bean问题=>{}", beanModel.getTagClass());
                if (tagertObj.getClass().getSimpleName().contains(beanModel.getTagClass().getSimpleName())) {
                    tagertObj = null;
                }
            } else {
                return tagertObj;
            }
        }
        log.debug("开始实例化:{}",beanModel);
        Object tmp = getTagertObjectCustom();
        if(tmp!=null && !inited) {
            inited = true;
//            if(attr!=null && attr.size()>0) {
//                attr.forEach((key,value)->{
//                    LazyBean.getInstance().setAttr(key, tmp, beanModel.getTagClass(), value);
//                });
//            }
            if(tagertObj instanceof InitializingBean) {
                try {
                    JunitInvokeUtil.invokeMethod(tagertObj, "afterPropertiesSet");
                } catch (SecurityException | IllegalArgumentException e) {
                    log.error("InitializingBean#afterPropertiesSet", e);
                    if(beanModel.isThrows()) {
                    	throw e;
                    }
                }
            }
        }
        return tmp;
    }
    
    protected abstract Object getTagertObjectCustom();

}
