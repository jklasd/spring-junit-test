package com.github.jklasd.test.lazybean.filter;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 监听类执行
 * @author jubin.zhang
 * @since 2021/05/22
 */
@Slf4j
public final class LazyBeanFilter {
	private LazyBeanFilter() {}
	private static Map<String,LazyConfigProcess> allMethodConfig = Maps.newHashMap();
	private static  Map<String,Map<String,LazyConfigProcess>> methodConfig = Maps.newHashMap();
	
	public final synchronized static void processLazyConfig(Object tagObj,Method method, Object[] param) {
		try {
			if(tagObj == null || method == null) {
				return;
			}
			
			allMethodConfig.entrySet().stream().filter(entry->entry.getKey().equals(tagObj.getClass().getName()))
			.forEach(entry->entry.getValue().process(tagObj, method, param));
			
			if(method!=null) {
				methodConfig.entrySet().stream().filter(entry -> entry.getKey().equals(tagObj.getClass().getName()))
				.forEach(entry->entry.getValue().entrySet().stream().filter(vEntry ->vEntry.getKey().equals(method.getName()))
						.forEach(vEntry->vEntry.getValue().process(tagObj, method, param)));
			}
		} catch (Exception e) {
			log.error("LazyBeanProcess#processLazyConfig=>{},=>{}",tagObj,method);
			log.error("LazyBeanProcess#processLazyConfig",e);
		}
	}
	public interface LazyConfigProcess{
		void process(Object tagObj,Method method, Object[] param);
	}
	public static synchronized void putAllMethod(String className,LazyConfigProcess process) {
		allMethodConfig.put(className, process);
	}
	public static synchronized void putMethod(String className,String methodStr,LazyConfigProcess process) {
		if(methodConfig.containsKey(className)) {
			methodConfig.get(className).put(methodStr, process);
		}else {
			methodConfig.put(className, Maps.newHashMap());
			methodConfig.get(className).put(methodStr, process);
		}
	}
	public interface LazyBeanInitProcess{
		void init(Map<String, Object> attrParam);
		void initMethod(Map<String,String> methods);
	}
	@Data
	public static class LazyBeanInitProcessImpl{
		private LazyBeanInitProcess process;
	}
	
}
