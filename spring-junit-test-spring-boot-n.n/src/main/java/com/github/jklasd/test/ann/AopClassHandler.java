package com.github.jklasd.test.ann;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.annotation.AspectJAdvisorFactory;
import org.springframework.aop.aspectj.annotation.BeanFactoryAspectInstanceFactory;
import org.springframework.aop.aspectj.annotation.MetadataAwareAspectInstanceFactory;
import org.springframework.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactory;

import com.github.jklasd.test.common.interf.handler.MockClassHandler;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
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
			
			/**
			 * 参考 BeanFactoryAspectJAdvisorsBuilder # buildAspectJAdvisors
			 */
			
//			AspectMetadata amd = new AspectMetadata(aopClass, BeanNameUtil.getBeanName(aopClass));
			String beanName = BeanNameUtil.getBeanName(aopClass);
//			LazyListableBeanFactory.getInstance().registerSingleton(beanName, );
			LazyBean.getInstance().createBeanForProxy(beanName, aopClass);
			MetadataAwareAspectInstanceFactory factory =
					new BeanFactoryAspectInstanceFactory(LazyListableBeanFactory.getInstance(), beanName);
			List<Advisor> classAdvisors = aspectJAdvisorFactory.getAdvisors(factory);
			
			advisorsHand(classAdvisors);
		}
	}

	private void advisorsHand(List<Advisor> classAdvisors) {
		/**
		 * @TODO 待处理
		 */
	}

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
