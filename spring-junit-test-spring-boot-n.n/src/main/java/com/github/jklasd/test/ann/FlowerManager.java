package com.github.jklasd.test.ann;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import javax.annotation.Nullable;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.springframework.context.ApplicationContext;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

import lombok.extern.slf4j.Slf4j;

/**
 * JUNIT 5
 * 
 * @author jubin.zhang
 *
 */
@Slf4j
public class FlowerManager
		implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor, BeforeEachCallback,
		AfterEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {

	/**
	 * {@link Namespace} in which {@code TestContextManagers} are stored,
	 * keyed by test class.
	 */
//	private static final Namespace NAMESPACE = Namespace.create(FlowerManager.class);


	/**
	 * Delegates to {@link TestContextManager#beforeTestClass}.
	 */
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
//		getTestContextManager(context).beforeTestClass();
		log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~初始化环境~~~~~~~~~~~~~~~~~~~~~~~~~~");
		TestUtil.resourcePreparation();
		log.info("~~~~~~~~~~~~~~~~~~~~~~~~初始化环境完毕~~~~~~~~~~~~~~~~~~~~~~~~~~");
	}

	/**
	 * Delegates to {@link TestContextManager#afterTestClass}.
	 */
	@Override
	public void afterAll(ExtensionContext context) throws Exception {
//		try {
//			getTestContextManager(context).afterTestClass();
//		}
//		finally {
//			getStore(context).remove(context.getRequiredTestClass());
//		}
		log.info("afterAll");
	}

	/**
	 * Delegates to {@link TestContextManager#prepareTestInstance}.
	 */
	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
//		getTestContextManager(context).prepareTestInstance(testInstance);
		log.info("-postProcessTestInstance-");
		LazyBean.getInstance().processAttr(testInstance, testInstance.getClass());
	}

	/**
	 * Delegates to {@link TestContextManager#beforeTestMethod}.
	 */
	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
//		getTestContextManager(context).beforeTestMethod(testInstance, testMethod);
		log.info("-beforeEach-");
	}

	/**
	 * Delegates to {@link TestContextManager#beforeTestExecution}.
	 */
	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
//		getTestContextManager(context).beforeTestExecution(testInstance, testMethod);
		
		log.info("-beforeTestExecution-");
	}

	/**
	 * Delegates to {@link TestContextManager#afterTestExecution}.
	 */
	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		Throwable testException = context.getExecutionException().orElse(null);
//		getTestContextManager(context).afterTestExecution(testInstance, testMethod, testException);
		
		log.info("-afterTestExecution-");
	}

	/**
	 * Delegates to {@link TestContextManager#afterTestMethod}.
	 */
	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		Throwable testException = context.getExecutionException().orElse(null);
//		getTestContextManager(context).afterTestMethod(testInstance, testMethod, testException);
		
		
		log.info("-afterEach-");
	}

	/**
	 * Determine if the value for the {@link Parameter} in the supplied {@link ParameterContext}
	 * should be autowired from the test's {@link ApplicationContext}.
	 * <p>A parameter is considered to be autowirable if one of the following
	 * conditions is {@code true}.
	 * <ol>
	 * <li>The {@linkplain ParameterContext#getDeclaringExecutable() declaring
	 * executable} is a {@link Constructor} and
	 * {@link TestConstructorUtils#isAutowirableConstructor(Constructor, Class)}
	 * returns {@code true}.</li>
	 * <li>The parameter is of type {@link ApplicationContext} or a sub-type thereof.</li>
	 * <li>{@link ParameterResolutionDelegate#isAutowirable} returns {@code true}.</li>
	 * </ol>
	 * <p><strong>WARNING</strong>: If a test class {@code Constructor} is annotated
	 * with {@code @Autowired} or automatically autowirable (see {@link TestConstructor}),
	 * Spring will assume the responsibility for resolving all parameters in the
	 * constructor. Consequently, no other registered {@link ParameterResolver}
	 * will be able to resolve parameters.
	 * @see #resolveParameter
	 * @see TestConstructorUtils#isAutowirableConstructor(Constructor, Class)
	 * @see ParameterResolutionDelegate#isAutowirable
	 */
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

	/**
	 * Resolve a value for the {@link Parameter} in the supplied {@link ParameterContext} by
	 * retrieving the corresponding dependency from the test's {@link ApplicationContext}.
	 * <p>Delegates to {@link ParameterResolutionDelegate#resolveDependency}.
	 * @see #supportsParameter
	 * @see ParameterResolutionDelegate#resolveDependency
	 */
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


	/**
	 * Get the {@link ApplicationContext} associated with the supplied {@code ExtensionContext}.
	 * @param context the current {@code ExtensionContext} (never {@code null})
	 * @return the application context
	 * @throws IllegalStateException if an error occurs while retrieving the application context
	 * @see org.springframework.test.context.TestContext#getApplicationContext()
	 */
	public static ApplicationContext getApplicationContext(ExtensionContext context) {
		return TestUtil.getInstance().getApplicationContext();
	}

	/**
	 * Get the {@link TestContextManager} associated with the supplied {@code ExtensionContext}.
	 * @return the {@code TestContextManager} (never {@code null})
	 */
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
