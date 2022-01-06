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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;

import com.github.jklasd.test.lazyplugn.spring.ConditionContextImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckUtil {
	private static Map<String, SpringBootCondition> checkBeanMap = Maps.newHashMap();
	static {
		Lists.newArrayList(ConditionalOnClass.class,ConditionalOnProperty.class,ConditionalOnBean.class).forEach(condition -> {
			Conditional conditonClass = condition.getAnnotation(Conditional.class);
			for (Class<?> checkClass : conditonClass.value()) {
				if (!checkBeanMap.containsKey(condition.getName())) {
					try {
						Constructor<?> constructor = checkClass.getDeclaredConstructors()[0];
						constructor.setAccessible(true);
						checkBeanMap.put(condition.getName(), (SpringBootCondition) constructor.newInstance());
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public static boolean check(Class<?> configClass) {
		// @ConditionalOnProperty
		// @ConditionalOnMissingBean
		return false;
	}

	private static ConditionContext context = new ConditionContextImpl();

	public static boolean checkClassExists(Class<?> configClass) {
		// @ConditionalOnClass
		try {
			Set<String> anns = AnnHandlerUtil.getInstance().loadAnnoName(configClass);
			if (anns == null) {
				return false;
			}
			for (String ann : anns) {
				if (checkBeanMap.containsKey(ann)) {
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
					SpringBootCondition condition = checkBeanMap.get(ann);
					ConditionOutcome outcome = condition.getMatchOutcome(context,
							AnnHandlerUtil.getInstance().getAnnotationMetadata(configClass));
					if (outcome.isMatch()) {
						log.debug("===通过校验===");
					}
					if(!outcome.isMatch()) {
						return outcome.isMatch();
					}
				}
			}
			return true;//没有condition校验
		} catch (IOException e) {
			log.error("checkClassExists", e);
		}
//		if(AnnHandlerUtil.isAnnotationPresent(configClass, ConditionalOnClass.class)) {
//			try {
////				Map<String, Object> attr = AnnHandlerUtil.getInstance().getAnnotationValue(configClass, ConditionalOnClass.class);
//				
//				Conditional conditonClass = ConditionalOnClass.class.getAnnotation(Conditional.class);
//				for(Class<?> checkClass : conditonClass.value()) {
//					if(!checkBeanMap.containsKey(checkClass)) {
//						initCheckBean(checkClass);
//					}
//					SpringBootCondition condition = checkBeanMap.get(checkClass);
//					ConditionOutcome outcome = condition.getMatchOutcome(context, AnnHandlerUtil.getInstance().getAnnotationMetadata(configClass));
//					if(outcome.isMatch()) {
//						log.debug("===通过校验===");
//					}
//					return outcome.isMatch();
//				}
//				
//			} catch (IOException e) {
//				log.error("checkClassExists",e);
//			}
//		}
		return false;
	}

	public static boolean checkClassExistsForMethod(Method method) {
		// @ConditionalOnClass
		if (AnnHandlerUtil.isAnnotationPresent(method, ConditionalOnClass.class)) {
			try {
				Conditional conditonClass = ConditionalOnClass.class.getAnnotation(Conditional.class);
				for (Class<?> checkClass : conditonClass.value()) {
					if (!checkBeanMap.containsKey(checkClass)) {
						initCheckBean(checkClass);
					}
					SpringBootCondition condition = checkBeanMap.get(checkClass);
					ConditionOutcome outcome = condition.getMatchOutcome(context,
							AnnHandlerUtil.getInstance().getAnnotationMetadata(method));
					if (outcome.isMatch()) {
						log.debug("===通过校验===");
					}
					return outcome.isMatch();
				}

			} catch (IOException e) {
				log.error("checkClassExists", e);
			}
		}
		return false;
	}

	private static synchronized void initCheckBean(Class<?> checkClass) {

	}

}
