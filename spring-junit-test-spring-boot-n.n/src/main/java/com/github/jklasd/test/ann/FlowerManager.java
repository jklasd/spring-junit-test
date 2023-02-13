package com.github.jklasd.test.ann;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import javax.annotation.Nullable;

import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.springframework.context.ApplicationContext;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.component.MockAnnHandlerComponent;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

import lombok.extern.slf4j.Slf4j;

/**
 * JUNIT 5
 * 参考 org.springframework.test.context.junit.jupiter.SpringExtension
 * @author jubin.zhang
 *
 */
@Slf4j
public class FlowerManager
		implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor, BeforeEachCallback,
		AfterEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {

//	private static final Namespace NAMESPACE = Namespace.create(FlowerManager.class);
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
//		getTestContextManager(context).beforeTestClass();
		log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~初始化环境~~~~~~~~~~~~~~~~~~~~~~~~~~");
		TestUtil.resourcePreparation();
		log.info("~~~~~~~~~~~~~~~~~~~~~~~~初始化环境完毕~~~~~~~~~~~~~~~~~~~~~~~~~~");
		Lifecycle testLif =	context.getTestInstanceLifecycle().get();
		if(testLif == Lifecycle.PER_CLASS) {//构造一遍，执行完所有测试方法
			MockAnnHandlerComponent.beforeAll(testInstance.getClass());
			LazyBean.getInstance().processAttr(testInstance, testInstance.getClass());
			MockAnnHandlerComponent.handlerClass(testInstance.getClass());
		}
		
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		log.info("afterAll");
		MockAnnHandlerComponent.releaseClass(context.getTestClass().get());
	}

	private Object testInstance;
	
	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
//		getTestContextManager(context).prepareTestInstance(testInstance);
		log.info("-postProcessTestInstance-");
		Lifecycle testLif =	context.getTestInstanceLifecycle().get();
		if(testLif == Lifecycle.PER_CLASS) {//构造一遍，执行完所有测试方法
			this.testInstance = testInstance;
		}else if(testLif == Lifecycle.PER_METHOD) {//每个方法构造一遍
			MockAnnHandlerComponent.beforeAll(testInstance.getClass());
			LazyBean.getInstance().processAttr(testInstance, testInstance.getClass());
			MockAnnHandlerComponent.handlerClass(testInstance.getClass());
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
//		getTestContextManager(context).beforeTestMethod(testInstance, testMethod);
		log.info("-----------------------------beforeEach-{}-{}---------------------------",testInstance.getClass().getSimpleName(),testMethod.getName());
	}

	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
//		getTestContextManager(context).beforeTestExecution(testInstance, testMethod);
		MockAnnHandlerComponent.handlerMethod(testMethod);
		log.info("-----------------------------beforeTestExecution-{}-{}---------------------------",testInstance.getClass().getSimpleName(),testMethod.getName());
	}

	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		Throwable testException = context.getExecutionException().orElse(null);
//		getTestContextManager(context).afterTestExecution(testInstance, testMethod, testException);
		
		log.info("-----------------------------afterTestExecution-{}-{}---------------------------",testInstance.getClass().getSimpleName(),testMethod.getName());
	}
	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		Throwable testException = context.getExecutionException().orElse(null);
//		getTestContextManager(context).afterTestMethod(testInstance, testMethod, testException);
		
		log.info("-----------------------------afterEach-{}-{}---------------------------",testInstance.getClass().getSimpleName(),testMethod.getName());
		MockAnnHandlerComponent.releaseMethod(testMethod);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Parameter parameter = parameterContext.getParameter();
		Executable executable = parameter.getDeclaringExecutable();
		Class<?> testClass = extensionContext.getRequiredTestClass();
//		return (TestConstructorUtils.isAutowirableConstructor(executable, testClass) ||
//				ApplicationContext.class.isAssignableFrom(parameter.getType()) ||
//				ParameterResolutionDelegate.isAutowirable(parameter, parameterContext.getIndex()));
		
		
		log.info("-supportsParameter-");
		
		return false;
	}

	@Override
	@Nullable
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Parameter parameter = parameterContext.getParameter();
		int index = parameterContext.getIndex();
		Class<?> testClass = extensionContext.getRequiredTestClass();
		ApplicationContext applicationContext = getApplicationContext(extensionContext);
//		return ParameterResolutionDelegate.resolveDependency(parameter, index, testClass,
//				applicationContext.getAutowireCapableBeanFactory());
		
		log.info("resolveParameter");
		
		return null;
	}


	public static ApplicationContext getApplicationContext(ExtensionContext context) {
		return TestUtil.getInstance().getApplicationContext();
	}

//	private static TestContextManager getTestContextManager(ExtensionContext context) {
//		Assert.notNull(context, "ExtensionContext must not be null");
//		Class<?> testClass = context.getRequiredTestClass();
//		Store store = getStore(context);
//		return store.getOrComputeIfAbsent(testClass, TestContextManager::new, TestContextManager.class);
//	}

//	private static Store getStore(ExtensionContext context) {
//		return context.getRoot().getStore(NAMESPACE);
//	}
}
