package com.github.jklasd.test.core.common.fieldann;

import java.lang.reflect.Field;
import java.util.Map;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.interf.ContainerRegister;
import com.github.jklasd.test.common.interf.handler.MockFieldHandlerI;
import com.github.jklasd.test.common.model.FieldDef;
import com.github.jklasd.test.lazybean.beanfactory.AbstractLazyProxy;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.google.common.collect.Maps;

public class MockFieldHandler implements ContainerRegister,MockFieldHandlerI{
	public static MockFieldHandler obj; 
	private MockFieldHandler() {};
	
	public static MockFieldHandler getInstance() {
		if(obj == null) {
			obj = new MockFieldHandler();
			ContainerManager.registComponent(obj);
		}
		return obj;
	}

	@Override
	public void register() {
		if(obj == null) {
			obj = this;
		}
		ContainerManager.registComponent(this);
	}

	public String getBeanKey() {
		return ContainerManager.NameConstants.MockFieldHandler;
	}

	@Override
	public void hand(Class<?> testClass) {
		if(!injectMocksObject.containsKey(testClass)) {
			return;
		}
		/**
		 * 查看当前类的InjectMocks field 处理
		 */
		injectMocksObject.get(testClass).entrySet().forEach(entry->{
			Object tagObject = entry.getKey();
			Class<?> mockClass = entry.getValue();
			
			Field[] fields = mockClass.getDeclaredFields();
			for(Field field : fields) {
				FieldAnnComponent.injeckMock(new MockFieldDef(field,tagObject,testClass));
			}
			
		});
	}

	@Override
	public void releaseClass(Class<?> testClass) {
		Map<Object, Class<?>> removeCache = injectMocksObject.remove(testClass);
		if(removeCache!=null) {
			removeCache.entrySet().forEach(entry->{
				LazyApplicationContext.getInstance().releaseBean(entry.getKey(), entry.getValue());
			});
		}
		cacheMockObject.remove(testClass);
	}

	Map<Class<?>,Map<Object,Class<?>>> injectMocksObject = Maps.newHashMap();
	
	public void load(Class<?> testClass, Object mock,Class<?> mockClass) {
		if(!injectMocksObject.containsKey(testClass)) {
			injectMocksObject.put(testClass, Maps.newConcurrentMap());
		}
		injectMocksObject.get(testClass).put(mock, mockClass);
	}

	
	Map<Class<?>,Map<Object,Class<?>>> cacheMockObject = Maps.newHashMap();
	public void registBean(String beanName, Object value, Class<?> type, Class<?> testClass) {
		if(!cacheMockObject.containsKey(testClass)) {
			cacheMockObject.put(testClass, Maps.newConcurrentMap());
		}
		cacheMockObject.get(testClass).put(value, type);
	}

	@Override
	public void injeckMock(FieldDef fieldDef) {
		MockFieldDef mockField = (MockFieldDef) fieldDef;
		Field field = fieldDef.getField();
		Class<?> testClass = mockField.getTetClass();
		cacheMockObject.get(testClass).entrySet().forEach(entry->{
			if(field.getType() == entry.getValue()) {
				Object tmp = fieldDef.getTagObj();
				if(AbstractLazyProxy.isProxy(tmp)) {
					tmp = AbstractLazyProxy.getProxyTagObj(tmp);
				}
				FieldAnnComponent.setObj(field, tmp, entry.getKey());
			}
		});
	}

}
