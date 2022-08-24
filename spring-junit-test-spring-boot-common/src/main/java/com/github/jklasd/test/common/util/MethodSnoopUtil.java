package com.github.jklasd.test.common.util;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.interf.register.BeanFactoryProcessorI;
import com.github.jklasd.test.common.interf.register.LazyBeanI;
import com.google.common.collect.Sets;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodSnoopUtil {
	
	
	/**
	 * 窥探静态方法的调用
	 * @author jubin.zhang
	 *
	 */
	class ExprEditorExt extends ExprEditor{
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
}
