package com.github.jklasd.test.ann;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.annotation.AspectJAdvisorFactory;
import org.springframework.aop.aspectj.annotation.BeanFactoryAspectInstanceFactory;
import org.springframework.aop.aspectj.annotation.MetadataAwareAspectInstanceFactory;
import org.springframework.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactory;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;

import com.github.jklasd.test.common.interf.handler.MockClassHandler;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.beanfactory.invoker.LazyAopInvoker;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.github.jklasd.test.util.BeanNameUtil;
import com.google.common.collect.Sets;

public class AopClassHandler implements MockClassHandler{
	
	Set<Class<?>> existsAop = Sets.newConcurrentHashSet();
	
	private AspectJAdvisorFactory aspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory();

	@Override
	public String getType() {
		return JunitSpecifyAop.class.getName();
	}

	@Override
	public void hand(Class<?> testClass) {
		JunitSpecifyAop aopBeanClass = testClass.getAnnotation(JunitSpecifyAop.class);
		aopClassHandler(aopBeanClass);
	}

	private void aopClassHandler(JunitSpecifyAop aopBeanClass) {
		for(Class<?> aopClass:aopBeanClass.value()) {
			if(existsAop.contains(aopClass)) {
				continue;
			}
			
			
			if(!aspectJAdvisorFactory.isAspect(aopClass)) {
				continue;
			}
			//控制重复
			existsAop.add(aopClass);
			
			/**
			 * 参考 BeanFactoryAspectJAdvisorsBuilder # buildAspectJAdvisors
			 */
			
//			AspectMetadata amd = new AspectMetadata(aopClass, BeanNameUtil.getBeanName(aopClass));
			String beanName = BeanNameUtil.getBeanName(aopClass);
//			LazyListableBeanFactory.getInstance().registerSingleton(beanName, );
//			LazyBean.getInstance().createBeanForProxy(beanName, aopClass);
			
			BeanModel model = new BeanModel();
		    model.setBeanName(beanName);
		    model.setTagClass(aopClass);
		    LazyBean.getInstance().buildProxy(model);
			
			MetadataAwareAspectInstanceFactory factory =
					new BeanFactoryAspectInstanceFactory(LazyListableBeanFactory.getInstance(), beanName);
			
			
			/**
			 * 获取环绕方法
			 */
			List<Advisor> classAdvisors = aspectJAdvisorFactory.getAdvisors(factory);
			
			//不需要排序
//			aspectJAwareAdvisorAutoProxyCreatorExt.sortAdvisors(classAdvisors);
			
			LazyAopInvoker.getInstance().regist(classAdvisors);
		}
	}
//	class AspectJAwareAdvisorAutoProxyCreatorExt extends AspectJAwareAdvisorAutoProxyCreator{
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = -3341013845208327389L;
//		
//		@Override
//		protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
//			return super.sortAdvisors(advisors);
//		}
//		
//	}

	@Override
	public void releaseClass(Class<?> testClass) {
		/**
		 * 不释放
		 */
	}

	@Override
	public void hand(Method testMethod) {
		JunitSpecifyAop aopBeanClass = testMethod.getAnnotation(JunitSpecifyAop.class);
		aopClassHandler(aopBeanClass);
	}

	@Override
	public void releaseMethod(Method testMethod) {
		/**
		 * 不释放
		 */
	}

	@Override
	public Boolean isMock() {
		return false;
	}

}
