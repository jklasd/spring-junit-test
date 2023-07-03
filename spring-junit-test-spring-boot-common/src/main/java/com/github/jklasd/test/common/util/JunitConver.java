package com.github.jklasd.test.common.util;

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;

import com.google.common.collect.Maps;
import com.sun.beans.editors.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JunitConver {
	
	private static TypeConverter converter = new SimpleTypeConverter();
	
	static Map<Class<?>,PropertyEditor> peMap = Maps.newHashMap();
	static {
		peMap.put(Boolean.class, new BooleanEditor());
		peMap.put(Integer.class, new IntegerEditor());
		peMap.put(Byte.class, new ByteEditor());
		peMap.put(Long.class, new LongEditor());
		peMap.put(Short.class, new ShortEditor());
		peMap.put(Double.class, new DoubleEditor());
	}
	
	public synchronized static Object converValue(String valueText,Class<?> type) {
		if(peMap.containsKey(type)) {
			peMap.get(type).setAsText(valueText);
			return peMap.get(type).getValue();
		}
		return valueText;
	}
	public static Object conver(Object values,Class<?> type,DependencyDescriptor descriptor) {

		if (type.isArray()) {
			Class<?> componentType = type.getComponentType();
			ResolvableType resolvableType = descriptor.getResolvableType();
			Class<?> resolvedArrayType = resolvableType.resolve(type);
			if (resolvedArrayType != type) {
				componentType = resolvableType.getComponentType().resolve();
			}
			if (componentType == null) {
				return null;
			}
			Object result = converter.convertIfNecessary(values, resolvedArrayType);
			return result;
		}
		else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
			Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
			if (elementType == null) {
				return null;
			}
			Object result = converter.convertIfNecessary(values, type);
			return result;
		}
		else if (Map.class == type) {
			ResolvableType mapType = descriptor.getResolvableType().asMap();
			Class<?> keyType = mapType.resolveGeneric(0);
			if (String.class != keyType) {
				return null;
			}
			Class<?> valueType = mapType.resolveGeneric(1);
			if (valueType == null) {
				return null;
			}
		}
	 return null;
	}
}
