package com.github.jklasd.test.lazybean.beanfactory.invoker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jklasd.test.common.util.MethodSnoopUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.beanfactory.LazyProxyManager;
import com.google.common.collect.Sets;

import javassist.CannotCompileException;

public class JunitInvoker extends AbstractProxyInvoker{

	@Override
	protected void exceptionInvoke(InvokeDTO dto, Map<String, Object> context, Exception e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void afterInvoke(InvokeDTO dto, Map<String, Object> context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void finallyInvoke(InvokeDTO dto, Map<String, Object> context) {
		// TODO Auto-generated method stub
		
	}

	Set<Method> handledMethods = Sets.newConcurrentHashSet();
	Set<String> handledClassName = Sets.newConcurrentHashSet();
	@Override
	protected boolean beforeInvoke(InvokeDTO dto, Map<String, Object> context)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(handledMethods.contains(dto.getMethod())) {
			return true;
		}
		try {
			List<String> className = MethodSnoopUtil.findNotPublicMethodForClass(dto.getMethod());
			if(className.isEmpty()) {
				return true;
			}
			handledMethods.add(dto.getMethod());
			
			Object obj = dto.getRealObj();
			//获取相关属性
			Class<?> tagClass = obj.getClass();
			Field[] fs = tagClass.getDeclaredFields();
			for(Field f : fs) {
				if(className.contains(f.getType().getName())) {
					if(handledClassName.contains(tagClass.getName()+className)) {
						continue;
					}
					handledClassName.add(tagClass.getName()+className);
					//处理
					if(!f.isAccessible()) {
						f.setAccessible(true);
					}
					Object tagObj = f.get(obj);
					if(LazyProxyManager.isProxy(tagObj)) {
						//process
						LazyBean.getInstance().processAttr(tagObj, f.getType(), false);
					}
				}
			}
			
		} catch (CannotCompileException e) {
			e.printStackTrace();
		}
		
		return true;
	}

	private static JunitInvoker invoker = new JunitInvoker();
	public static AbstractProxyInvoker getInstance() {
		return invoker;
	}

}
