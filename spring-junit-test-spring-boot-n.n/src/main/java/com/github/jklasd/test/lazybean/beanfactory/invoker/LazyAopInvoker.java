package com.github.jklasd.test.lazybean.beanfactory.invoker;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.Pointcut;

import com.github.jklasd.test.lazybean.filter.LazyBeanFilter;
import com.github.jklasd.test.spring.suppert.AopContextSuppert;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyAopInvoker extends AbstractProxyInvoker{
	private static LazyAopInvoker instance;
	public static LazyAopInvoker getInstance() {
		if(instance == null) {
			synchronized (LazyAopInvoker.class) {
				if(instance == null) {
					instance = new LazyAopInvoker();
				}
			}
		}
		return instance;
	}
	private List<Pointcut> allPoint = Lists.newArrayList();
	
	private Set<Pointcut> cachePoint = Sets.newHashSet();
	
	public void regist(Set<Pointcut> pointc) {
		pointc.stream().forEach(item->{
			if(cachePoint.contains(item)) {//AspectJExpressionPointcut hashcode 已被重写
				return;
			}
			cachePoint.add(item);
			allPoint.add(item);
		});
	}

	@Override
	protected void afterInvoke(InvokeDTO dto, Map<String, Object> context) {
		
	}

	@Override
	protected void finallyInvoke(InvokeDTO dto, Map<String, Object> context) {
		AopContextSuppert.setProxyObj(context.get("oldObj"));
	}

	@Override
	protected boolean beforeInvoke(InvokeDTO dto, Map<String, Object> context)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object newObj = dto.getRealObj();
		Object oldObj = AopContextSuppert.getProxyObject();
		context.put("oldObj", oldObj);
        AopContextSuppert.setProxyObj(dto.getPoxy());
        LazyBeanFilter.processLazyConfig(newObj, dto.getMethod(), dto.getParam());
		return false;
	}

	@Override
	protected void exceptionInvoke(InvokeDTO dto, Map<String, Object> context, Exception e) {
		
	}

}
