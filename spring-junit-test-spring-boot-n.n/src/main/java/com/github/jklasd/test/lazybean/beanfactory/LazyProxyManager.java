package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Factory;

import com.github.jklasd.test.lazybean.beanfactory.invoker.LazyAopInvoker;
import com.github.jklasd.test.lazybean.beanfactory.invoker.ProxyInvoker;
import com.github.jklasd.test.lazybean.beanfactory.invoker.TransferInvoker;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class LazyProxyManager {
	private static ThreadLocal<Map<String,Object>> lastInvoker = new ThreadLocal<Map<String,Object>>();
	public static void setLastInvoker(Class<?> lastClass,Method lastMethod) {
		Map<String,Object> tmpInvokerInfo = Maps.newHashMap();
    	tmpInvokerInfo.put("class", lastClass);
    	tmpInvokerInfo.put("method", lastMethod.getName());
    	lastInvoker.set(tmpInvokerInfo);
	}
	
	public static Map<String, Object> getLastInvoker() {
		return lastInvoker.get();
	}
	
	public static void setLastInvoker(Map<String, Object> lastInvokerInfo) {
		lastInvoker.set(lastInvokerInfo);
	}

	
	/************************************代理类判别工具********************************************/
	
	public static boolean isProxy(Object obj){
		return obj instanceof Proxy || obj instanceof Factory;
    }
	
    public static Object getProxyTagObj(Object obj){
    	if(isProxy(obj)) {
			
			if(obj instanceof Proxy) {
				LazyImple imple = (LazyImple) Proxy.getInvocationHandler(obj);
    			return imple.getTagertObj();
    		}else {
    			Factory fa = (Factory) obj;
    			Callback[] cbs = fa.getCallbacks();
    			LazyCglib cglib = (LazyCglib) cbs[0];
    			return cglib.getTagertObj();
    		}
		}
    	return obj;
    }
    public static Class<?> getProxyTagClass(Object obj){
    	try {
    		if(isProxy(obj)) {
    			if(obj instanceof Proxy) {
    				InvocationHandler ih = Proxy.getInvocationHandler(obj);
    				if(ih instanceof LazyImple) {
    					LazyImple imple = (LazyImple) Proxy.getInvocationHandler(obj);
    					return imple.getBeanModel().getTagClass();
    				}else {
    					return null;
    				}
        		}else {
        			Factory fa = (Factory) obj;
        			Callback[] cbs = fa.getCallbacks();
        			LazyCglib cglib = (LazyCglib) cbs[0];
        			return cglib.getBeanModel().getTagClass();
        		}
    		}
			return obj.getClass();
		} catch (SecurityException | IllegalArgumentException e) {
			return obj.getClass();
		}
    }
    
    public static Object instantiateProxy(Object obj) {
		return getProxyTagObj(obj);
	}
	public static BaseAbstractLazyProxy getProxy(Object obj) {
		if(isProxy(obj)) {
			if(obj instanceof Proxy) {
				LazyImple imple = (LazyImple) Proxy.getInvocationHandler(obj);
    			return imple;
    		}else {
    			Factory fa = (Factory) obj;
    			Callback[] cbs = fa.getCallbacks();
    			LazyCglib cglib = (LazyCglib) cbs[0];
    			return cglib;
    		}
		}
		return null;
	}

	private static ProxyInvoker proxyInvoker;
	public static ProxyInvoker getProxyInvoker() {
		if(proxyInvoker != null) {
			return proxyInvoker;
		}
		bruildInvokerList();
		return proxyInvoker;
	}

	private synchronized static ProxyInvoker bruildInvokerList() {
        if(proxyInvoker == null) {
        	List<ProxyInvoker> pis = Lists.newArrayList(
        			LazyAopInvoker.getInstance(),
        			TransferInvoker.getInstance());
        	proxyInvoker = pis.get(0);
        	ProxyInvoker point = proxyInvoker;
        	for(int i=1;i<pis.size();i++) {
        		point.setNextInvoker(pis.get(i));
        		point = pis.get(i);
        	}
        }
		return proxyInvoker;
	}
}
