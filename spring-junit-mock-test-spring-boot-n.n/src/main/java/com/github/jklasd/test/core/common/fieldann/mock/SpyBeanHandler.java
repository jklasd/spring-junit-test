package com.github.jklasd.test.core.common.fieldann.mock;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.ResolvableType;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.interf.register.LazyBeanI;
import com.github.jklasd.test.common.model.FieldDef;
import com.github.jklasd.test.common.util.ScanUtil;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpyBeanHandler extends MockHandler implements FieldHandler{
	private Class<?> SpyDefinition = ScanUtil.loadClass(packagePath+".SpyDefinition");
	Method createSpy;
	Constructor<?> spyDefStructor;
	private LazyBeanI lazyBean;
	{
		try {
			Constructor<?>[] structors = SpyDefinition.getDeclaredConstructors();
			spyDefStructor = structors[0];
			spyDefStructor.setAccessible(true);
			
			createSpy = SpyDefinition.getDeclaredMethod("createSpy", Object.class);
			createSpy.setAccessible(true);
		} catch (SecurityException | IllegalArgumentException | NoSuchMethodException e) {
			e.printStackTrace();
		}
		lazyBean = ContainerManager.getComponent(LazyBeanI.class.getSimpleName());
	}
	
	@Override
	public void handler(FieldDef def ,Annotation ann) {
		Field attr = def.getField();
		Object obj = def.getTagObj();
		SpyBean spyAnn = (SpyBean) ann;
		Qualifier qualifier = attr.getAnnotation(Qualifier.class);
		try {
			Object qualDef = qualDefStructor.newInstance(attr,Sets.newHashSet(spyAnn));
			
			
			Object spyDefObj = spyDefStructor.newInstance(spyAnn.name(),
					ResolvableType.forClass(attr.getType()),
					spyAnn.reset(),spyAnn.proxyTargetAware(),
					qualDef);
			String beanName = spyAnn.name();
			if(qualifier!=null) {
				beanName = qualifier.value();
			}
			Object value = createSpy.invoke(spyDefObj, lazyBean.buildProxy(attr.getType(),beanName));
			FieldAnnComponent.setObj(attr, obj, value);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	public String getType() {
		return SpyBean.class.getName();
	}

}
