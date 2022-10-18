package com.github.jklasd.test.common.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.interf.register.BeanFactoryProcessorI;
import com.github.jklasd.test.common.interf.register.LazyBeanI;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodSnoopUtil {
	
	
	/**
	 * 窥探静态方法的调用
	 * @author jubin.zhang
	 *
	 */
	class PryStaticMethod extends ExprEditor{
		AnnHandlerUtil annHandler = AnnHandlerUtil.getInstance();
		private BeanFactoryProcessorI beanFactoryProcessor = ContainerManager.getComponent(BeanFactoryProcessorI.class.getSimpleName());
		private LazyBeanI lazyBean = ContainerManager.getComponent(LazyBeanI.class.getSimpleName());
		Set<String> cacheTagClass = Sets.newConcurrentHashSet();
		public void edit(MethodCall m) throws CannotCompileException{
			try {
				if(!cacheTagClass.contains(m.getClassName())
						&& !m.getMethodName().startsWith("$") 
						&& m.getMethod().getModifiers() == Modifier.STATIC) {
					
					cacheTagClass.add(m.getClassName());
					
					Class<?> tagClass = ScanUtil.loadClass(m.getClassName());
					AnnotationMetadata meta = annHandler.getAnnotationMetadata(tagClass);
					if(meta.hasAnnotation(Component.class.getName())
							|| meta.hasAnnotation(Service.class.getName())
							|| meta.hasAnnotation(Configuration.class.getName())
							|| meta.hasAnnotation(Repository.class.getName())
							) {
						if(beanFactoryProcessor.notBFProcessor(tagClass) && CheckUtil.checkProp(tagClass)) {
							lazyBean.processStatic(tagClass);
						}
					}
				}
			} catch (NotFoundException | IOException e) {
				log.warn("NotFoundException:{}",e.getMessage());
			}
		}
	}
	
	
	static class PryNotPubilcMethod extends ExprEditor{
		
		@Getter
		private List<String> findToDo = Lists.newArrayList();
		
		public void edit(MethodCall m) throws CannotCompileException{
			try {
				if(Modifier.isFinal(m.getMethod().getModifiers())//如果是final
						|| !Modifier.isPublic(m.getMethod().getModifiers())) {//如果不是公开方法
					String cName = m.getClassName();
					String mName = m.getMethodName();
					String params = m.getSignature();
					log.debug("params:{},name:{},of:{}",params,mName,cName);
					findToDo.add(m.getClassName());
				}
			} catch (NotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static List<String> findNotPublicMethodForClass(Method method) throws CannotCompileException{
		CtMethod ctMethod = getCtMethod(method);
		PryNotPubilcMethod tmp = new PryNotPubilcMethod();
		ctMethod.instrument(tmp);
		return tmp.getFindToDo();
	}
	
	
	
	static ClassPool cp = ClassPool.getDefault();
	public static CtMethod getCtMethod(Method method) {
		try {
			CtClass ctClass = cp.get(method.getDeclaringClass().getName());//自带缓存
			if(method.getParameterCount()>0) {
				Class<?>[] paramTypes = method.getParameterTypes();
				CtClass[] paramCts = toCtClass(paramTypes);
				ctClass.getDeclaredMethod(method.getName(), paramCts);
			}
			return ctClass.getDeclaredMethod(method.getName());
		} catch (NotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	private static CtClass[] toCtClass(Class<?>[] paramTypes) throws NotFoundException {
		CtClass[] result = new CtClass[paramTypes.length];
		for(int i=0;i<paramTypes.length;i++) {
			result[i] = cp.get(paramTypes[i].getName());//自带缓存;
		}
		return result;
	}
}
