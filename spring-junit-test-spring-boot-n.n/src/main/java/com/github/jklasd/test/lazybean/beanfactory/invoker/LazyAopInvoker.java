package com.github.jklasd.test.lazybean.beanfactory.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectJAfterAdvice;
import org.springframework.aop.aspectj.AspectJAfterReturningAdvice;
import org.springframework.aop.aspectj.AspectJAroundAdvice;
import org.springframework.aop.aspectj.AspectJMethodBeforeAdvice;

import com.github.jklasd.test.common.model.BeanModel;
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
	
	private List<AspectJMethodBeforeAdvice> beforeAdv = Lists.newArrayList();
	@Override
	protected boolean beforeInvoke(InvokeDTO dto, Map<String, Object> context)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object newObj = dto.getRealObj();
		Object oldObj = AopContextSuppert.getProxyObject();
		context.put("oldObj", oldObj);
        AopContextSuppert.setProxyObj(dto.getPoxy());
        LazyBeanFilter.processLazyConfig(newObj, dto.getMethod(), dto.getParam());
        
        beforeAdv.stream().filter(item->item.getPointcut().matches(dto.getMethod(), dto.getRealObj().getClass(), dto.getParam())).forEach(item->{
        	try {
				item.before(dto.getMethod(), dto.getParam(), dto.getRealObj());
			} catch (Throwable e) {
				e.printStackTrace();
			}
        });
        
		return false;
	}
	
	protected Object roundInvoke(Object poxy, Method method, Object[] param,BeanModel beanModel,Object realObj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, Throwable {
		return super.roundInvoke(poxy, method, param, beanModel, realObj);
	}

	private List<AspectJAfterAdvice> afterAdv = Lists.newArrayList();
	@Override
	protected void afterInvoke(InvokeDTO dto, Map<String, Object> context) {
		afterAdv.stream().filter(item->item.getPointcut().matches(dto.getMethod(), dto.getRealObj().getClass(), dto.getParam())).forEach(item->{
        	try {
				item.invoke(null);
			} catch (Throwable e) {
				e.printStackTrace();
			}
        });
	}

	@Override
	protected void finallyInvoke(InvokeDTO dto, Map<String, Object> context) {
		AopContextSuppert.setProxyObj(context.get("oldObj"));
	}

	
	

	@Override
	protected void exceptionInvoke(InvokeDTO dto, Map<String, Object> context, Exception e) {
		
	}
	
	private List<AspectJAroundAdvice> aroundAdv = Lists.newArrayList();
	
	private List<AspectJAfterReturningAdvice> returnAdv = Lists.newArrayList();
	
	public void regist(List<Advisor> classAdvisors) {
		classAdvisors.forEach(adv->{
			Advice advice = adv.getAdvice();
			if(advice instanceof AbstractAspectJAdvice) {
				if(advice instanceof AspectJAfterReturningAdvice) {
					returnAdv.add((AspectJAfterReturningAdvice) advice);
				}
				if(advice instanceof AspectJAfterAdvice) {
					afterAdv.add((AspectJAfterAdvice) advice);
				}
				if(advice instanceof AspectJAroundAdvice) {
					aroundAdv.add((AspectJAroundAdvice) advice);
				}
				if(advice instanceof AspectJMethodBeforeAdvice) {
					beforeAdv.add((AspectJMethodBeforeAdvice) advice);
				}
			}
		});
	}

}
