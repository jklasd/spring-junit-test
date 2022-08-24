package com.github.jklasd.test.common.util;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.interf.register.ConditionClassManagerI;
import com.github.jklasd.test.common.interf.register.JunitCoreComponentI;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

/**
 * 需要加載完处理
 * @author jubin.zhang
 *
 */
@Slf4j
public class CheckUtil {
	private static Map<String, SpringBootCondition> checkClassMap = Maps.newHashMap();
	private static Map<String, SpringBootCondition> checkPropMap = Maps.newHashMap();
	private static Map<String, SpringBootCondition> otherMap = Maps.newHashMap();
	static {
		ConditionClassManagerI mananger = ContainerManager.getComponent(ConditionClassManagerI.class.getSimpleName());
		
		loadCondition(checkClassMap, mananger.getCheckClassArr());
		loadCondition(checkPropMap,mananger.getCheckPropArr());
		loadCondition(otherMap, mananger.getOtherCheckArr());
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
	public static class ConditionContextImpl implements ConditionContext{

		private JunitCoreComponentI bean = ContainerManager.getComponent(JunitCoreComponentI.class.getSimpleName());
		
		@Override
		public BeanDefinitionRegistry getRegistry() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ConfigurableListableBeanFactory getBeanFactory() {
			return bean.getApplicationContext().getBeanFactory();
		}

		@Override
		public Environment getEnvironment() {
			return bean.getApplicationContext().getEnvironment();
		}

		@Override
		public ResourceLoader getResourceLoader() {
			// TODO Auto-generated method stub
			return bean.getApplicationContext();
		}

		@Override
		public ClassLoader getClassLoader() {
			return JunitClassLoader.getInstance();
		}

	}

	private static ConditionContext context = new ConditionContextImpl();

	public static boolean checkOther(Class<?> configClass) {
		return checkCommonMethod(configClass,otherMap);
	}
	
	public static boolean checkProp(Class<?> configClass) {
		return checkCommonMethod(configClass,otherMap);
	}

	private static boolean checkCommonMethod(Class<?> configClass,Map<String, SpringBootCondition> checkMap) {
		try {
			Set<String> anns = AnnHandlerUtil.getInstance().loadAnnoName(configClass);
			if (anns == null) {
				return false;
			}
			for (String ann : anns) {
				if (checkMap.containsKey(ann)) {
					SpringBootCondition condition = checkMap.get(ann);
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
