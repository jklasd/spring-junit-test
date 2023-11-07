package com.github.jklasd.test.core.common.fieldann;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.mockito.internal.creation.bytebuddy.MockAccess;
import org.mockito.mock.MockCreationSettings;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.interf.handler.MockFieldHandlerI;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.model.FieldDef;
import com.github.jklasd.test.lazybean.beanfactory.LazyProxyManager;
import com.github.jklasd.test.util.BeanNameUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockFieldHandler implements MockFieldHandlerI {
	public static MockFieldHandler obj;

	private MockFieldHandler() {
	};

	public static MockFieldHandler getInstance() {
		if (obj == null) {
			obj = new MockFieldHandler();
			ContainerManager.registComponent(obj);
		}
		return obj;
	}

	public String getBeanKey() {
		return MockFieldHandlerI.class.getName();
	}

	ThreadLocal<String> testClassId = new InheritableThreadLocal<>();

	@Override
	public void hand(Class<?> testClass) {
		if (!injectMocksObject.containsKey(testClassId.get())) {
			return;
		}
		injectMocksClass.put(getId(), Sets.newHashSet());
		injectMocksClass.get(getId()).add(testClass);
		Class<?> tmpClass = testClass;
		while (tmpClass.getSuperclass() != Object.class) {
			injectMocksClass.get(getId()).add(tmpClass.getSuperclass());
			tmpClass = tmpClass.getSuperclass();
		}
		/**
		 * 查看当前类的InjectMocks field 处理
		 */
		injectMocksObject.get(testClassId.get()).entrySet().forEach(entry -> {
			String beanName = entry.getKey();
			Object mockBean = entry.getValue();

			MockAccess detail = (MockAccess) mockBean;
			MockCreationSettings<?> setting = detail.getMockitoInterceptor().getMockHandler().getMockSettings();
			Class<?> mockClass = setting.getTypeToMock();
			Field[] fields = mockClass.getDeclaredFields();
			for (Field field : fields) {
				FieldAnnComponent.injeckMock(new MockFieldDef(field, mockBean, testClass, beanName));
			}

		});
		injectMocksObjectForClass.get(getId()).entrySet().forEach(entry->{

			Class<?> mockClass = entry.getKey();
			Object mockBean = entry.getValue();

			Field[] fields = mockClass.getDeclaredFields();
			for (Field field : fields) {
				FieldAnnComponent.injeckMock(new MockFieldDef(field, mockBean, testClass, null));
			}
		});
	}

	private String getId() {
		String uuid = testClassId.get();
		if(uuid == null) {
			log.debug("uuid 为空");
		}
		return uuid;
	}

	Map<String, Set<Class<?>>> injectMocksClass = Maps.newHashMap();

	@Override
	public void releaseClass(Class<?> testClass) {
		injectMocksObject.remove(getId());
		cacheMockObject.remove(getId());
		injectMocksClass.remove(getId());
		testClassId.remove();
	}

	Map<String, Map<String, Object>> injectMocksObject = Maps.newHashMap();
	Map<String, Map<Class<?>, Object>> injectMocksObjectForClass = Maps.newHashMap();

	public void load(Class<?> testClass, Object mock, Class<?> mockClass, String bName) {
		if (!injectMocksObject.containsKey(testClassId.get())) {
			injectMocksObject.put(testClassId.get(), Maps.newConcurrentMap());
			injectMocksObjectForClass.put(getId(), Maps.newConcurrentMap());
		}
		if(bName!=null) {
			injectMocksObject.get(testClassId.get()).put(bName, mock);
		}else {
			injectMocksObjectForClass.get(getId()).put(mockClass, mock);
		}
	}

	Map<String, Map<Object, Class<?>>> cacheMockObject = Maps.newHashMap();

	public void registBean(String beanName, Object value, Class<?> type, Class<?> testClass) {
		if (!cacheMockObject.containsKey(testClassId.get())) {
			cacheMockObject.put(testClassId.get(), Maps.newConcurrentMap());
		}
		cacheMockObject.get(testClassId.get()).put(value, type);
	}

	@Override
	public void injeckMock(FieldDef fieldDef) {
//		MockFieldDef mockField = (MockFieldDef) fieldDef;
		Field field = fieldDef.getField();
		if(!cacheMockObject.containsKey(testClassId.get())) {
			return;
		}
//		Class<?> testClass = mockField.getTetClass();
		cacheMockObject.get(testClassId.get()).entrySet().forEach(entry -> {
			if (field.getType() == entry.getValue()) {
				Object tmp = fieldDef.getTagObj();
				if (LazyProxyManager.isProxy(tmp)) {
					tmp = LazyProxyManager.getProxyTagObj(tmp);
				}
				FieldAnnComponent.setObj(field, tmp, entry.getKey());
			}
		});
	}

	@Override
	public void registId() {
		testClassId.set(UUID.randomUUID().toString());
	}

	@Override
	public Object getMockBean(Class<?> tagClass, String name) {
		if (name == null) {
			name = BeanNameUtil.getBeanName(tagClass);
		}
		return cacheMockObject.get(testClassId.get()).get(name);
	}

	@Override
	public boolean finded(BeanModel beanModel) {
		String bName = beanModel.getBeanName();
		if(getId()!=null) {
			if (bName!=null
					&& injectMocksObject.get(getId())!=null
					&& injectMocksObject.get(getId()).containsKey(bName)) {
				Object tmp = injectMocksObject.get(testClassId.get()).get(bName);
				return tmp!=null;
			}else if(injectMocksObjectForClass.get(getId())!=null &&
					injectMocksObjectForClass.get(getId()).containsKey(beanModel.getTagClass())) {
				Object tmp = injectMocksObjectForClass.get(getId()).get(beanModel.getTagClass());
				return tmp!=null;
			}
		}
		return false;
	}

	@Override
	public Object invoke(Object poxy, Method method, Object[] param,BeanModel beanModel) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String bName = beanModel.getBeanName();
		if(getId()!=null) {
			if (bName!=null
					&& injectMocksObject.get(getId())!=null
					&& injectMocksObject.get(getId()).containsKey(bName)) {
				Object tmp = injectMocksObject.get(testClassId.get()).get(bName);
				return method.invoke(tmp, param);
			}else if(injectMocksObjectForClass.get(getId()).containsKey(beanModel.getTagClass())) {
				Object tmp = injectMocksObjectForClass.get(getId()).get(beanModel.getTagClass());
				return method.invoke(tmp, param);
			}
		}
		return null;
	}

}
