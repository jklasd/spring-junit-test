package com.github.jklasd.test;

import java.util.List;

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunSpringJunitTest extends BlockJUnit4ClassRunner{
	public Class<?> runClass;
	public RunSpringJunitTest(Class<?> runClass) throws InitializationError {
		super(runClass);
		this.runClass = runClass;
	}

	@Override
	public Description getDescription() {
		return super.getDescription();
//		return Description.createTestDescription(runClass, "spring-junit-test");
	}

	@Override
	public void run(RunNotifier notifier) {
//		log.debug("=======加载环境=======");
		TestUtil.resourcePreparation();
//		log.debug("=======加载结束=======");
		super.run(notifier);
	}
	
	protected Statement methodBlock(FrameworkMethod method) {
		Object test;
        try {
            test = new ReflectiveCallable() {
                @Override
                protected Object runReflectiveCall() throws Throwable {
                    return createTest();
                }
            }.run();
        } catch (Throwable e) {
            return new Fail(e);
        }
        log.debug("=======初始化=【{}】======",test.getClass());
        //注入当前执行对象
  		LazyBean.getInstance().processAttr(test, test.getClass());

        Statement statement = methodInvoker(method, test);
        statement = possiblyExpectingExceptions(method, test, statement);
        statement = withPotentialTimeout(method, test, statement);
        statement = withBefores(method, test, statement);
        statement = withAfters(method, test, statement);
        statement = withRules(method, test, statement);
        return statement;
    }
	private Statement withRules(FrameworkMethod method, Object target,
            Statement statement) {
        List<TestRule> testRules = getTestRules(target);
        Statement result = statement;
        result = withMethodRules(method, testRules, target, result);
        result = withTestRules(method, testRules, result);

        return result;
    }
	private Statement withTestRules(FrameworkMethod method, List<TestRule> testRules,
            Statement statement) {
        return testRules.isEmpty() ? statement :
                new RunRules(statement, testRules, describeChild(method));
    }
	private Statement withMethodRules(FrameworkMethod method, List<TestRule> testRules,
            Object target, Statement result) {
        for (org.junit.rules.MethodRule each : rules(target)) {
            if (!testRules.contains(each)) {
                result = each.apply(result, method, target);
            }
        }
        return result;
    }
}
