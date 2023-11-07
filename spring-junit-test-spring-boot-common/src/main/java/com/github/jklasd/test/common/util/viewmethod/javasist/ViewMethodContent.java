package com.github.jklasd.test.common.util.viewmethod.javasist;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.Contants;
import com.github.jklasd.test.common.interf.register.BeanFactoryProcessorI;
import com.github.jklasd.test.common.util.AnnHandlerUtil;
import com.github.jklasd.test.common.util.CheckUtil;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.common.util.viewmethod.PryMethodInfoI;
import com.google.common.base.Objects;
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
public class ViewMethodContent {

	static ClassPool cp = ClassPool.getDefault();
	
	/**
	 * 待后期优化，静态类访问问题
	 * @author jubin.zhang
	 *
	 */
	public static class PryMethodInfo extends ExprEditor implements PryMethodInfoI{
		private BeanFactoryProcessorI beanFactoryProcessor = ContainerManager.getComponent(BeanFactoryProcessorI.class.getSimpleName());
		@Getter
		private Set<String> findToFinal = Sets.newConcurrentHashSet();
		@Getter
		private Set<Class<?>> findToStatic = Sets.newConcurrentHashSet();
		private Method method;
		public PryMethodInfo(Method method) {
			this.method = method;
		}
		private CtMethod ctmethod;
		public PryMethodInfo(CtMethod method) {
			this.ctmethod = method;
		}
		@Getter
		Set<String> filtered = Sets.newConcurrentHashSet();
		
		Set<String> recursion = Sets.newConcurrentHashSet();
		
		public void edit(MethodCall m) throws CannotCompileException{
			try {
				String mName = m.getMethodName();
				String cName = m.getMethod().getDeclaringClass().getName();
//				if(Objects.equal("getBean", mName)) {
//					log.info("Cc:{}",cName);
//				}
				if(filtered.contains(cName)) {//过滤不需要处理的
					return;
				}
				String recursionKey = cName+"#"+mName;
				if(recursion.contains(recursionKey)) {
					return;
				}
				String localC = null;
				String mName_tmp = null;
				if(method == null) {
					localC = ctmethod.getDeclaringClass().getName();
					mName_tmp = ctmethod.getMethodInfo().getName();
				}else {
					localC = method.getDeclaringClass().getName();
					mName_tmp = method.getName();
				}
				recursion.add(recursionKey);
				if(Objects.equal(cName, localC) 
						&& !Objects.equal(mName_tmp,mName)) {
					//调用本类方法,继续查看
					log.debug("调用本类方法,继续查看");
					CtMethod ctMethod = m.getMethod();
					PryMethodInfo tmp = new PryMethodInfo(m.getMethod());
					tmp.filtered.addAll(filtered);//防止递归
					tmp.recursion.addAll(recursion);//防止递归
					ctMethod.instrument(tmp);
					
					findToStatic.addAll(tmp.getFindToStatic());
					findToFinal.addAll(tmp.getFindToFinal());
					return;
				}
				Class<?> tagClass = ScanUtil.loadClass(cName);
				if(Contants.runPrepareStatic) {
					if(!m.getMethodName().startsWith("$") 
							&& Modifier.isStatic(m.getMethod().getModifiers())
							&& Modifier.isPublic(m.getMethod().getModifiers())) {
						
						CtMethod ctMethod = m.getMethod();
						PryMethodInfo tmp = new PryMethodInfo(m.getMethod());
						tmp.filtered.addAll(filtered);//防止递归
						tmp.recursion.addAll(recursion);//防止递归
						ctMethod.instrument(tmp); // 存在静态方法中调用静态方法，然后调用spring 容器获取对象
						
						findToStatic.addAll(tmp.getFindToStatic());
						findToFinal.addAll(tmp.getFindToFinal());
						if(AnnHandlerUtil.isAnnotationPresent(tagClass, Component.class)) {
							if(beanFactoryProcessor.notBFProcessor(tagClass) && CheckUtil.checkProp(tagClass)) {
								findToStatic.add(tagClass);
							}
						}
					}
				}
				if(AnnHandlerUtil.isAnnotationPresent(tagClass, Component.class)) {
					
					if(Modifier.isFinal(m.getMethod().getModifiers())//如果是final
							|| !Modifier.isPublic(m.getMethod().getModifiers())) {//如果不是公开方法
						String params = m.getSignature();
						log.debug("params:{},name:{},of:{}",params,mName,cName);
						
						if(!localC.equals(cName)) {
							findToFinal.add(m.getClassName());
						}
					}
					
				}else {
					filtered.add(cName);
				}
			} catch (NotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static PryMethodInfoI findNotPublicMethodForClass(Method method) throws CannotCompileException{
		CtMethod ctMethod = getCtMethod(method);
		if(ctMethod == null) {
			return new PryMethodInfo(ctMethod);
		}
		PryMethodInfo tmp = new PryMethodInfo(method);
		ctMethod.instrument(tmp);
		
		CtMethod[] ctms = ctMethod.getDeclaringClass().getDeclaredMethods();
		for(CtMethod ci:ctms) {
			if(ci.getName().startsWith("lambda$")) {//处理匿名方法内容
				PryMethodInfo tmpi = new PryMethodInfo(ci);
				tmpi.filtered.addAll(tmp.filtered);
				tmpi.recursion.addAll(tmp.recursion);
				ci.instrument(tmpi);
				tmp.getFindToFinal().addAll(tmpi.findToFinal);
				tmp.getFindToStatic().addAll(tmpi.findToStatic);
			}
		}
		
		return tmp;
	}
	
	private static CtMethod getCtMethod(Method method) {
		try {
			
			CtClass ctClass = cp.get(method.getDeclaringClass().getName());//自带缓存
			if(method.getParameterCount()>0) {
				Class<?>[] paramTypes = method.getParameterTypes();
				CtClass[] paramCts = toCtClass(paramTypes);
				return ctClass.getDeclaredMethod(method.getName(), paramCts);
			}
			return ctClass.getDeclaredMethod(method.getName());
		} catch (NotFoundException e) {
//			e.printStackTrace();
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
