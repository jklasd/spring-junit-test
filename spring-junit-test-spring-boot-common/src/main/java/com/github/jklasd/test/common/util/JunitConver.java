package com.github.jklasd.test.common.util;

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;

import com.google.common.collect.Maps;
import com.sun.beans.editors.BooleanEditor;
import com.sun.beans.editors.ByteEditor;
import com.sun.beans.editors.DoubleEditor;
import com.sun.beans.editors.FloatEditor;
import com.sun.beans.editors.IntegerEditor;
import com.sun.beans.editors.LongEditor;
import com.sun.beans.editors.ShortEditor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JunitConver {
	
	private static TypeConverter converter = new SimpleTypeConverter();
	
	static Map<Class<?>,PropertyEditor> peMap = Maps.newHashMap();
	static {
		peMap.put(boolean.class, new BooleanEditor());
		peMap.put(Boolean.class, new BooleanEditor());
		peMap.put(byte.class, new ByteEditor());
		peMap.put(Byte.class, new ByteEditor());
//		ColorEditor.class
		peMap.put(double.class, new DoubleEditor());
		peMap.put(Double.class, new DoubleEditor());
//		EnumEditor.class
		peMap.put(Float.class, new FloatEditor());
		peMap.put(float.class, new FloatEditor());
//		FontEditor.class
		peMap.put(int.class, new IntegerEditor());
		peMap.put(Integer.class, new IntegerEditor());
		peMap.put(long.class, new LongEditor());
		peMap.put(Long.class, new LongEditor());
//		NumberEditor.class
		peMap.put(short.class, new ShortEditor());
		peMap.put(Short.class, new ShortEditor());
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
