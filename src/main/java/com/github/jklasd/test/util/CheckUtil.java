package com.github.jklasd.test.util;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;

import com.github.jklasd.test.lazyplugn.spring.ConditionContextImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckUtil {
	private static Map<String, SpringBootCondition> checkClassMap = Maps.newHashMap();
	private static Map<String, SpringBootCondition> checkPropMap = Maps.newHashMap();
	static {
		loadCondition(checkClassMap,ConditionalOnClass.class,ConditionalOnBean.class,ConditionalOnMissingClass.class);
		loadCondition(checkPropMap,ConditionalOnProperty.class,ConditionalOnResource.class);
	}

	protected static void loadCondition(Map<String, SpringBootCondition> checkClassMap, Class<?>... classes) {
		Lists.newArrayList(classes).forEach(condition -> {
			Conditional conditonClass = condition.getAnnotation(Conditional.class);
			for (Class<?> checkClass : conditonClass.value()) {
				if (!checkClassMap.containsKey(condition.getName())) {
					try {
						Constructor<?> constructor = checkClass.getDeclaredConstructors()[0];
						constructor.setAccessible(true);
						checkClassMap.put(condition.getName(), (SpringBootCondition) constructor.newInstance());
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	private static ConditionContext context = new ConditionContextImpl();

	public static boolean checkProp(Class<?> configClass) {
		try {
			Set<String> anns = AnnHandlerUtil.getInstance().loadAnnoName(configClass);
			if (anns == null) {
				return false;
			}
			for (String ann : anns) {
				if (checkPropMap.containsKey(ann)) {
					SpringBootCondition condition = checkPropMap.get(ann);
					ConditionOutcome outcome = condition.getMatchOutcome(context,
							AnnHandlerUtil.getInstance().getAnnotationMetadata(configClass));
					if(!outcome.isMatch()) {
						return outcome.isMatch();
					}
				}
			}
			return true;//没有condition校验
		} catch (IOException e) {
			log.error("checkClassExists", e);
		}
		return false;
	}
	
	public static boolean checkClassExists(Class<?> configClass) {
		// @ConditionalOnClass
		try {
			Set<String> anns = AnnHandlerUtil.getInstance().loadAnnoName(configClass);
			if (anns == null) {
				return false;
			}
			for (String ann : anns) {
				if (checkClassMap.containsKey(ann)) {
					if(ann.equals(ConditionalOnBean.class.getName())) {//这里校验class是否存在
						Map<String, Object> attr = AnnHandlerUtil.getInstance().getAnnotationValue(configClass, ConditionalOnBean.class);
						String[] classes = (String[]) attr.get("value");
						for(String name : classes) {
							Class<?> forBeanClass = ScanUtil.loadClass(name);
							if(forBeanClass==null) {
								return false;
							}
						}
						continue;
					}
					SpringBootCondition condition = checkClassMap.get(ann);
					ConditionOutcome outcome = condition.getMatchOutcome(context,
							AnnHandlerUtil.getInstance().getAnnotationMetadata(configClass));
//					if (outcome.isMatch()) {
//						log.debug("===通过校验===");
//					}
					if(!outcome.isMatch()) {
						return outcome.isMatch();
					}
				}
			}
			return true;//没有condition校验
		} catch (IOException e) {
			log.error("checkClassExists", e);
		}
		return false;
	}

	public static boolean checkClassExistsForMethod(Method method) {
		// @ConditionalOnClass
		if (AnnHandlerUtil.isAnnotationPresent(method, ConditionalOnClass.class)) {
		}
		return false;
	}

}
