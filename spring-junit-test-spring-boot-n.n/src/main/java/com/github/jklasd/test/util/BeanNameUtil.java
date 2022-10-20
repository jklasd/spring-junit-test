package com.github.jklasd.test.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.common.exception.JunitException;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanNameUtil {

	public synchronized static String getBeanNameForMethod(Method method, Class<?> tagClass) {
		if(method.getAnnotation(Bean.class)!=null) {
			Bean ann = method.getAnnotation(Bean.class);
			if(ann.value().length>0) {
				return ann.value()[0];
			}
			return method.getName();
		}
		return getBeanName(tagClass);
	}
	
	public synchronized static String getBeanName(Class<?> classBean) {
		boolean finded = findedFormAnno(classBean);
		if(finded) {
			String beanName = getBeanNameFormAnno(classBean);
			if(StringUtils.isNotBlank(beanName)) {
				return beanName;
			}
		}
		if(classBean.isInterface()) {
			return null;
		}
		if(classBean.getSimpleName().length()<1) {
		    return null;
		}
		if(finded) {
			return classBean.getSimpleName().substring(0,1).toLowerCase()+classBean.getSimpleName().substring(1);
		}
		return classBean.getName();
	}
	
	static List<Class<? extends Annotation>> list = Lists.newArrayList(Component.class,Service.class,Repository.class,Controller.class);
	
	public synchronized static boolean findedFormAnno(Class<?> classBean) {
		for(Class<? extends Annotation> annClass : list) {
			Annotation ann = classBean.getAnnotation(annClass);
			if(ann!=null) {
				return true;
			}
		}
		return false;
	}
	
	public synchronized static String getBeanNameFormAnno(Class<?> classBean) {
		try {
			for(Class<? extends Annotation> annClass : list) {
				Annotation ann = classBean.getAnnotation(annClass);
				if(ann == null) {
					continue;
				}
				Method value = annClass.getDeclaredMethod("value");
				Object name = value.invoke(ann);
				if(name!=null && StringUtils.isNotBlank(name.toString())) {
					return name.toString();
				}
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new JunitException(e, true);
		}
		return null;
	}

	public static String fixedBeanName(Class<?> tagClass) {
		String beanName = getBeanName(tagClass);
		return beanName == null?tagClass.getName():beanName;
	}
}
