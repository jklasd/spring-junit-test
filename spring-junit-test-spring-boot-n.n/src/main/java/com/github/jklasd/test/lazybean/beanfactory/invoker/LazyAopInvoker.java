package com.github.jklasd.test.lazybean.beanfactory.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.ReflectiveMethodInvocation;

import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazybean.filter.LazyBeanFilter;
import com.github.jklasd.test.spring.suppert.AopContextSuppert;

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
	
	protected Object superRoundInvoke(Object poxy, Method method, Object[] param,BeanModel beanModel,Object realObj) throws Throwable {
		return super.roundInvoke(poxy, method, param, beanModel, realObj);
	}
	
	protected Object roundInvoke(Object poxy, Method method, Object[] param,BeanModel beanModel,Object realObj) throws  Throwable {
		List<Object> advList = advised.getInterceptorsAndDynamicInterceptionAdvice(method, beanModel.getTagClass());
		
		if(advList.isEmpty()) {
			return super.roundInvoke(poxy, method, param, beanModel, realObj);
		}
		
		
		return new MethodInvocationImpl(poxy, realObj, method, param, beanModel.getTagClass(), 
				advList
				, new Callable() {
						@Override
						public Object call() throws Throwable {
							//回到下一个Invoker执行
							return superRoundInvoke(poxy, method, param, beanModel, realObj);
						}
					}
		).proceed();
	}

	/**
	 * 正常执行
	 */
	@Override
	protected void afterInvoke(InvokeDTO dto, Map<String, Object> context) {
		
	}

	/**
	 * 强制执行
	 */
//	private List<AspectJAfterAdvice> afterAdv = Lists.newArrayList();
	@Override
	protected void finallyInvoke(InvokeDTO dto, Map<String, Object> context) {
		AopContextSuppert.setProxyObj(context.get("oldObj"));
	}

	
	
//	private List<AspectJAfterThrowingAdvice> throwingAdv = Lists.newArrayList();
	@Override
	protected void exceptionInvoke(InvokeDTO dto, Map<String, Object> context, Exception e) {
	}
	
	private AdvisedSupport advised = new AdvisedSupport();
	/**
	 * Advisor 顺序 由 Advisor自身执行顺序控制
	 * @param classAdvisors
	 */
	public void regist(List<Advisor> classAdvisors) {
		advised.addAdvisors(classAdvisors);
	}
	public interface Callable{
		public Object call() throws Throwable;
	}
	
	class MethodInvocationImpl extends ReflectiveMethodInvocation{
		
		private Callable callAble;
		
		protected MethodInvocationImpl(Object proxy, Object target, Method method, Object[] arguments,
				Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers,Callable callAble) {
			super(proxy, target, method, arguments, targetClass, interceptorsAndDynamicMethodMatchers);
			this.callAble = callAble;
		}
		
		@Override
		public Object proceed() throws Throwable {
			return super.proceed();
		}
		
		@Override
		protected Object invokeJoinpoint() throws Throwable {
			return callAble.call();
		}
	}
}
