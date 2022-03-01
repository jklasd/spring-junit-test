package com.github.jklasd.test.core.common.fieldann;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.ResolvableType;

import com.github.jklasd.test.core.common.FieldAnnUtil;
import com.github.jklasd.test.core.common.FieldAnnUtil.FieldHandler;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpyBeanHandler  implements FieldHandler{

	String packagePath = "org.springframework.boot.test.mock.mockito";
	private Class<?> SpyDefinition = ScanUtil.loadClass(packagePath+".SpyDefinition");
	private Class<?> QualifierDefinition = ScanUtil.loadClass(packagePath+".QualifierDefinition");
//	Object definitionsParserObj;
	Method createSpy;
	Constructor<?> spyDefStructor;
	Constructor<?> qualDefStructor;
//	private MockitoPostProcessor MockitoPostProcessor = new MockitoPostProcessor();
	{
		try {
			Constructor<?>[] structors = SpyDefinition.getDeclaredConstructors();
			spyDefStructor = structors[0];
			spyDefStructor.setAccessible(true);
			
			createSpy = SpyDefinition.getDeclaredMethod("createSpy", Object.class);
			createSpy.setAccessible(true);
			
			Constructor<?>[] qstructors = QualifierDefinition.getDeclaredConstructors();
			qualDefStructor = qstructors[0];
			qualDefStructor.setAccessible(true);
		} catch (SecurityException | IllegalArgumentException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void handler(FieldDef def) {
		Field attr = def.getField();
		Object obj = def.getTagObj();
		SpyBean spyAnn = attr.getAnnotation(SpyBean.class);
		try {
			Object qualDef = qualDefStructor.newInstance(attr,Sets.newHashSet(spyAnn));
			
			
			Object spyDefObj = spyDefStructor.newInstance(spyAnn.name(),
					ResolvableType.forClass(attr.getType()),
					spyAnn.reset(),spyAnn.proxyTargetAware(),
					qualDef);
			
			Object value = createSpy.invoke(spyDefObj, LazyBean.getInstance().buildProxy(attr.getType(),spyAnn.name()));
			FieldAnnUtil.setObj(attr, obj, value);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	public String getType() {
		return SpyBean.class.getName();
	}

}
