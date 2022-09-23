package com.github.jklasd.test.lazybean.beanfactory.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazybean.beanfactory.LazyProxyManager;
import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractProxyInvoker implements ProxyInvoker{
	
	@Getter
	@AllArgsConstructor
	public class InvokeDTO{
		Object poxy;
		Method method;
		Object[] param;
		BeanModel beanModel;
		Object realObj;
	}

	protected ProxyInvoker nextInvoker;
	
	@Override
	public void setNextInvoker(ProxyInvoker invoker) {
		this.nextInvoker = invoker;
	}

	public Object invoke(Object poxy, Method method, Object[] param,BeanModel beanModel,Object realObj) throws Throwable {
		Map<String,Object> lastInvokerInfo = LazyProxyManager.getLastInvoker();
		Map<String,Object> context = Maps.newHashMap();
		InvokeDTO dto = new InvokeDTO(poxy, method, param, beanModel, realObj);
        try {
        	LazyProxyManager.setLastInvoker(beanModel.getTagClass(), method);
        	/**
        	 * 执行前处理
        	 */
        	beforeInvoke(dto,context);
        	
        	Object obj = roundInvoke(poxy, method, param, beanModel, realObj);
        	/**
        	 * 执行后处理
        	 */
        	afterInvoke(dto,context);
        	
        	return obj;
        }catch (JunitException e) {
        	log.warn("LazyCglib#intercept warn.lastInvoker=>{}", lastInvokerInfo);
        	//异常处理
        	exceptionInvoke(dto,context,e);
        	throw e;
        }catch (InvocationTargetException e) {
        	//异常处理
        	exceptionInvoke(dto,context,e);
        	throw e;
		}catch (Exception e) {
        	log.warn("LazyCglib#intercept warn.lastInvoker=>{}", lastInvokerInfo);
            log.error("LazyCglib#intercept ERROR=>{}#{}==>message:{},params:{}", beanModel.getTagClass(), method.getName(),e.getMessage());
            //异常处理
            exceptionInvoke(dto,context,e);
            throw e;
        }finally {
        	/**
        	 * finally处理
        	 */
        	finallyInvoke(dto,context);
        	LazyProxyManager.setLastInvoker(lastInvokerInfo);
		}
	}

	protected abstract void exceptionInvoke(InvokeDTO dto, Map<String, Object> context, Exception e);

	protected abstract void afterInvoke(InvokeDTO dto, Map<String, Object> context);

	protected Object roundInvoke(Object poxy, Method method, Object[] param,BeanModel beanModel,Object realObj) throws Exception, Throwable {
		return nextInvoker!=null ? nextInvoker.invoke(poxy, method, param, beanModel, realObj)
    			: method.invoke(realObj, param);
	}
	
	protected abstract void finallyInvoke(InvokeDTO dto, Map<String, Object> context);

	protected abstract boolean beforeInvoke(InvokeDTO dto,Map<String,Object> context) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;


}