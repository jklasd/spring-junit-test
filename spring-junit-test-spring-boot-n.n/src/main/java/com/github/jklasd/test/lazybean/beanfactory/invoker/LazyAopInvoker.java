package com.github.jklasd.test.lazybean.beanfactory.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJAfterAdvice;
import org.springframework.aop.aspectj.AspectJAfterReturningAdvice;
import org.springframework.aop.aspectj.AspectJAfterThrowingAdvice;
import org.springframework.aop.aspectj.AspectJMethodBeforeAdvice;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AdvisorChainFactory;
import org.springframework.aop.framework.DefaultAdvisorChainFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.lang.Nullable;

import com.github.jklasd.test.common.model.BeanModel;
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
	
//	private Method aspectMethod_invokeAdviceMethod1;
//	private Method aspectMethod_getJoinPointMatch;
//	{
//		try {
//			Class<?> JoinPointMatch = ScanUtil.loadClass("org.aspectj.weaver.tools.JoinPointMatch");
//			//@Nullable JoinPointMatch jpMatch, @Nullable Object returnValue, @Nullable Throwable ex
//			aspectMethod_invokeAdviceMethod1 = AbstractAspectJAdvice.class.getDeclaredMethod("invokeAdviceMethod", new Class[] {JoinPointMatch,Object.class,Throwable.class});
//			aspectMethod_getJoinPointMatch = AbstractAspectJAdvice.class.getDeclaredMethod("getJoinPointMatch");
//		} catch (NoSuchMethodException | SecurityException e1) {
//			e1.printStackTrace();
//		}
//	}
	
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
//		Object newObj = dto.getRealObj();
//		Object oldObj = AopContextSuppert.getProxyObject();
//		context.put("oldObj", oldObj);
//        AopContextSuppert.setProxyObj(dto.getPoxy());
//        LazyBeanFilter.processLazyConfig(newObj, dto.getMethod(), dto.getParam());
//        
//        beforeAdv.stream().filter(item->item.getPointcut().matches(dto.getMethod(), dto.getRealObj().getClass(), dto.getParam())).forEach(item->{
//        	try {
//				item.before(dto.getMethod(), dto.getParam(), dto.getRealObj());
//			} catch (Throwable e) {
//				e.printStackTrace();
//			}
//        });
        
		return false;
	}
//	private List<AspectJAroundAdvice> aroundAdv = Lists.newArrayList();
	
	protected Object superRoundInvoke(Object poxy, Method method, Object[] param,BeanModel beanModel,Object realObj) throws Throwable {
		return super.roundInvoke(poxy, method, param, beanModel, realObj);
	}
	
	protected Object roundInvoke(Object poxy, Method method, Object[] param,BeanModel beanModel,Object realObj) throws  Throwable {
		findAspect(method, beanModel.getTagClass(), param);
		return new MethodInvocationImpl(poxy, realObj, method, param, beanModel.getTagClass(), null, new Callable() {
			@Override
			public Object call() throws Throwable {
				return superRoundInvoke(poxy, method, param, beanModel, realObj);
			}
		}).proceed();
	}

	private void findAspect(Method method, Class<?> tagClass, Object[] param) {
		
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
	private List<AspectJAfterAdvice> afterAdv = Lists.newArrayList();
	@Override
	protected void finallyInvoke(InvokeDTO dto, Map<String, Object> context) {
//		afterAdv.stream().filter(item->item.getPointcut().matches(dto.getMethod(), dto.getRealObj().getClass(), dto.getParam())).forEach(item->{
//        	try {
////				item.invoke(null);
//        		Object joinPoint = aspectMethod_getJoinPointMatch.invoke(item);
//        		aspectMethod_invokeAdviceMethod1.invoke(item, new Object[] {joinPoint,null,null});
//			} catch (Throwable e) {
//				throw new JunitException("aop "+item.getAspectName()+"-Method:"+item.getAspectJAdviceMethod().getName(),e,true);
//			}
//        });
		AopContextSuppert.setProxyObj(context.get("oldObj"));
	}

	
	
	private List<AspectJAfterThrowingAdvice> throwingAdv = Lists.newArrayList();
	@Override
	protected void exceptionInvoke(InvokeDTO dto, Map<String, Object> context, Exception e) {
//		throwingAdv.stream().filter(item->item.getPointcut().matches(dto.getMethod(), dto.getRealObj().getClass(), dto.getParam())).forEach(item->{
////			item.invoke(null);
////			invokeAdviceMethod(getJoinPointMatch(), null, ex);
//			try {
//				Object joinPoint = aspectMethod_getJoinPointMatch.invoke(item);
//				aspectMethod_invokeAdviceMethod1.invoke(item, new Object[] {joinPoint,null,e});
//			}catch (Throwable ex) {
//				throw new JunitException("aop "+item.getAspectName()+"-Method:"+item.getAspectJAdviceMethod().getName(),ex,true);
//			}
//        });
	}
	
	private List<AspectJAfterReturningAdvice> returnAdv = Lists.newArrayList();
	
	public void regist(List<Advisor> classAdvisors) {
//		classAdvisors.forEach(adv->{
//			Advice advice = adv.getAdvice();
//			if(advice instanceof AbstractAspectJAdvice) {
//				if(advice instanceof AspectJAfterReturningAdvice) {
//					returnAdv.add((AspectJAfterReturningAdvice) advice);
//				}
//				if(advice instanceof AspectJAfterAdvice) {
//					afterAdv.add((AspectJAfterAdvice) advice);
//				}
//				if(advice instanceof AspectJAroundAdvice) {
//					aroundAdv.add((AspectJAroundAdvice) advice);
//				}
//				if(advice instanceof AspectJMethodBeforeAdvice) {
//					beforeAdv.add((AspectJMethodBeforeAdvice) advice);
//				}
//				if(advice instanceof AspectJAfterThrowingAdvice) {
//					throwingAdv.add((AspectJAfterThrowingAdvice)advice);
//				}
//			}
//		});
	}
	public interface Callable{
		public Object call() throws Throwable;
	}
	class AspectJAwareAdvisorAutoProxyCreatorExt extends AspectJAwareAdvisorAutoProxyCreator{

		/**
		 * 
		 */
		private static final long serialVersionUID = -3341013845208327389L;
		
		@Override
		protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
			return super.sortAdvisors(advisors);
		}
		
	}
	class AdvisedSupportExt extends AdvisedSupport{
		AdvisorChainFactory advisorChainFactory = new DefaultAdvisorChainFactory();
		/**
		 * 
		 */
		private static final long serialVersionUID = -5372871399151914658L;
		private transient Map<MethodCacheKey, List<Object>> methodCache;
		@Override
		public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) {
			MethodCacheKey cacheKey = new MethodCacheKey(method);
			List<Object> cached = methodCache.get(cacheKey);
			if (cached == null) {
				cached = advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
						this, method, targetClass);
				methodCache.put(cacheKey, cached);
			}
			return cached;
		}
		private final class MethodCacheKey implements Comparable<MethodCacheKey> {

			private final Method method;

			private final int hashCode;

			public MethodCacheKey(Method method) {
				this.method = method;
				this.hashCode = method.hashCode();
			}

			@Override
			public boolean equals(@Nullable Object other) {
				return (this == other || (other instanceof MethodCacheKey &&
						this.method == ((MethodCacheKey) other).method));
			}

			@Override
			public int hashCode() {
				return this.hashCode;
			}

			@Override
			public String toString() {
				return this.method.toString();
			}

			@Override
			public int compareTo(MethodCacheKey other) {
				int result = this.method.getName().compareTo(other.method.getName());
				if (result == 0) {
					result = this.method.toString().compareTo(other.method.toString());
				}
				return result;
			}
		}
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
