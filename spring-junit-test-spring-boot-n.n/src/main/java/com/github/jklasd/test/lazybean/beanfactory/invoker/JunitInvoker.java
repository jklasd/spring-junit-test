package com.github.jklasd.test.lazybean.beanfactory.invoker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.Contants;
import com.github.jklasd.test.common.interf.register.LazyBeanI;
import com.github.jklasd.test.common.util.MethodSnoopUtil;
import com.github.jklasd.test.common.util.viewmethod.PryMethodInfoI;
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
	Set<Class<?>> staticClassName = Sets.newConcurrentHashSet();
	private LazyBeanI lazyBean = ContainerManager.getComponent(LazyBeanI.class.getSimpleName());
	@Override
	protected boolean beforeInvoke(InvokeDTO dto, Map<String, Object> context)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method tagMethod = dto.getMethod();
		if(dto.getBeanModel().getTagClass().isInterface()
				|| Modifier.isAbstract(dto.getBeanModel().getTagClass().getModifiers())) {//处理接口方法和抽象类方法
			try {
				tagMethod = dto.getRealObj().getClass().getDeclaredMethod(tagMethod.getName(), tagMethod.getParameterTypes());
			} catch (NoSuchMethodException | SecurityException e) {
			}
		}
		if(handledMethods.contains(tagMethod)) {
			return true;
		}
		try {

			
			PryMethodInfoI methodInfo = MethodSnoopUtil.findNotPublicMethodForClass(tagMethod);
			handledMethods.add(tagMethod);
			if(Contants.runPrepareStatic) {
				if(!methodInfo.getFindToStatic().isEmpty()) {
					//处理静态方法
					methodInfo.getFindToStatic().forEach(tagClass->lazyBean.processStatic(tagClass));
				}
			}
			if(methodInfo.getFindToFinal().isEmpty()) {
				return true;
			}
			
			Object obj = dto.getRealObj();
			//获取相关属性
			Class<?> tagClass = obj.getClass();
			Field[] fs = tagClass.getDeclaredFields();
			for(Field f : fs) {
				if(methodInfo.getFindToFinal().contains(f.getType().getName())) {
					if(handledClassName.contains(tagClass.getName()+f.getType().getName())) {
						continue;
					}
					handledClassName.add(tagClass.getName()+f.getType().getName());
					//处理
					if(!f.isAccessible()) {
						f.setAccessible(true);
					}
					Object tagObj = f.get(obj);
					if(LazyProxyManager.isProxy(tagObj)) {
						//process
						LazyBean.getInstance().processAttr(tagObj, f.getType());
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
