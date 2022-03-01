package com.github.jklasd.test.core.common.fieldann;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.mock.mockito.MockitoPostProcessor;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.core.facade.JunitClassLoader;
import com.github.jklasd.test.util.ScanUtil;

public class MockHandler{
//	String packagePath = "org.springframework.boot.test.mock.mockito";
//	private Class<?> DefinitionsParser = ScanUtil.loadClass(packagePath+".DefinitionsParser");
//	private Object definitionsParserObj;
//	private Method parse;
//	private Method getDefinitions;
//	{
//		Constructor<?>[] constructors = DefinitionsParser.getDeclaredConstructors();
//		try {
//			for(Constructor<?> structor : constructors) {
//				if(structor.getParameterCount()==0) {
//					structor.setAccessible(true);
//					definitionsParserObj = structor.newInstance();
//					break;
//				}
//			}
//			
//			Method[] ms = DefinitionsParser.getDeclaredMethods();
//			for(Method method : ms) {
//				if(method.getName().equals("parse")) {
//					parse = method;
//					parse.setAccessible(true);
//				}else if(method.getName().equals("getDefinitions")){
//					getDefinitions = method;
//					getDefinitions.setAccessible(true);
//				}
//			}
//			
//		} catch (Exception e) {
//		}
//	}
//	public void handler(Class<?> unitClass) {
//		try {
//			parse.invoke(definitionsParserObj, unitClass);
////			//Set<Definition> definitions
//			Object obj = getDefinitions.invoke(definitionsParserObj);
//			Constructor<?>[] constructs = MockitoPostProcessor.class.getConstructors();
//			MockitoPostProcessor processor = (MockitoPostProcessor) constructs[0].newInstance(obj);
//			processor.setBeanClassLoader(JunitClassLoader.getInstance());
//			processor.postProcessBeanFactory(TestUtil.getInstance().getApplicationContext().getDefaultListableBeanFactory());
//		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException e) {
//			e.printStackTrace();
//		}
//	}
}
