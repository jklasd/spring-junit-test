package com.github.jklasd.test;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.github.jklasd.test.common.component.MockAnnHandlerComponent;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

/**
 * JUNIT 4
 * @author jubin.zhang
 *
 */
@Slf4j
public class RunSpringJunitTest extends BlockJUnit4ClassRunner{
	public Class<?> runClass;
	Object test;
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
		log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~初始化环境~~~~~~~~~~~~~~~~~~~~~~~~~~");
		TestUtil.resourcePreparation();
		log.info("~~~~~~~~~~~~~~~~~~~~~~~~初始化环境完毕~~~~~~~~~~~~~~~~~~~~~~~~~~");
		super.run(notifier);
	}
	protected Statement withBeforeClasses(Statement statement) {
		try {
			Method action = FrameworkMethodExt.class.getMethod("action");
			FrameworkMethod processAttrMethod = new FrameworkMethod(action);
			return new RunBefores(statement,Lists.newArrayList(processAttrMethod), new FrameworkMethodExt() {
				@Override
				public void action() {
					if(test == null) {
						try {
							test = runClass.newInstance();
						} catch (InstantiationException | IllegalAccessException e) {
							e.printStackTrace();
						}
					}
					//H2使用判断
			        MockAnnHandlerComponent.beforeAll(runClass);
			        LazyBean.getInstance().processAttr(test, runClass);
					MockAnnHandlerComponent.handlerClass(runClass);
				}
			});
		} catch (Exception e) {
			log.error("withBeforeClasses",e);
		}
		return statement;
    }
	protected Statement withAfterClasses(Statement statement) {
		try {
			Method action = FrameworkMethodExt.class.getMethod("action");
			FrameworkMethod processAttrMethod = new FrameworkMethod(action);
			return new RunAfters(statement,Lists.newArrayList(processAttrMethod), new FrameworkMethodExt() {
				@Override
				public void action() {
					MockAnnHandlerComponent.releaseClass(runClass);
				}
			});
		} catch (Exception e) {
			log.error("withAfterClasses",e);
		}
		return statement;
	}
	/**
	 * 构建方法执行顺序
	 */
	protected Statement methodBlock(FrameworkMethod method) {
		if(test == null) {
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
		}
        /*
         * 执行方法体
         */
        Statement statement = new InvokeMethodExt(method,test);//5
        statement = possiblyExpectingExceptions(method, test, statement);
//        statement = withPotentialTimeout(method, test, statement);
        /**
         * 执行前，处理
         */
        //执行顺序
//        statement = withBefores(method, test, statement);//单元测试自定义执行方法前执行内容4
//        statement = withBeforesForAfterProcessAttr(method, test, statement);//3
//        statement = processAttr(method, test, statement);//2
//        statement = withBeforeProcessAttr(method, test, statement);//1
        /**
         * 执行后处理
         */
        statement = withAfters(method, test, statement);//6
//        statement = withAftersForJunit(method, test, statement);//7
        
        statement = withRules(method, test, statement);
        return statement;
    }
//	/**
//	 * 在注入测试实例属性前执行【执行测试方法体前】
//	 * @param method
//	 * @param test
//	 * @param statement
//	 * @return
//	 */
//	private Statement withBeforeProcessAttr(FrameworkMethod method, Object test, Statement statement) {
//		try {
//			Method action = FrameworkMethodExt.class.getMethod("action");
//			FrameworkMethod processAttrMethod = new FrameworkMethod(action);
//			return new RunAfters(statement,Lists.newArrayList(processAttrMethod), new FrameworkMethodExt() {
//				@Override
//				public void action() {
//					MockAnnHandlerComponent.releaseMethod(method.getMethod());
//				}
//			});
//		} catch (Exception e) {
//			log.error("withBeforesForAfterProcessAttr",e);
//		}
//		return statement;
//	}
//	/**
//	 * 在注入测试实例属性前执行【执行测试方法体前】
//	 * @param method
//	 * @param test
//	 * @param statement
//	 * @return
//	 */
//	private Statement withBeforesForAfterProcessAttr(FrameworkMethod method, Object test, Statement statement) {
//		try {
//			Method action = FrameworkMethodExt.class.getMethod("action");
//			FrameworkMethod processAttrMethod = new FrameworkMethod(action);
//			return new RunBefores(statement,Lists.newArrayList(processAttrMethod), new FrameworkMethodExt() {
//				@Override
//				public void action() {
//					MockAnnHandlerComponent.handlerClass(test.getClass());
//				}
//			});
//		} catch (Exception e) {
//			log.error("withBeforesForAfterProcessAttr",e);
//		}
//		return statement;
//	}
//	private Statement withAftersForJunit(FrameworkMethod method, Object test, Statement statement) {
//		try {
//			Method action = FrameworkMethodExt.class.getMethod("action");
//			FrameworkMethod processAttrMethod = new FrameworkMethod(action);
//			return new RunBefores(statement,Lists.newArrayList(processAttrMethod), new FrameworkMethodExt() {
//				@Override
//				public void action() {
//					MockAnnHandlerComponent.handlerMethod(method.getMethod());
//				}
//			});
//		} catch (Exception e) {
//			log.error("withBeforesForAfterProcessAttr",e);
//		}
//		return statement;
//	}
	
	public interface FrameworkMethodExt{
		public void action();
	}
	
	public class InvokeMethodExt extends Statement{
		private final FrameworkMethod testMethod;
	    private final Object target;
	    
	    public InvokeMethodExt(FrameworkMethod testMethod, Object target) {
	        this.testMethod = testMethod;
	        this.target = target;
	    }
		@Override
		public void evaluate() throws Throwable {
			try {
				log.info("-----------------------------执行测试方法-{}-{}---------------------------",target.getClass().getSimpleName(),testMethod.getName());
				MockAnnHandlerComponent.handlerMethod(testMethod.getMethod());
				testMethod.invokeExplosively(target);
			} finally {
				MockAnnHandlerComponent.releaseMethod(testMethod.getMethod());
				log.info("-----------------------------执行测试方法-{}-{}--结束-------------------------",target.getClass().getSimpleName(),testMethod.getName());
			}
		}
		
	}
	
//	public class ProcessTagretObj extends FrameworkMethod{
//
//		private Object targetObj;
//		
//		public ProcessTagretObj(Method method, Object target) {
//			super(method);
//			this.targetObj = target;
//		}
//
//		public void processAttr() {
//			LazyBean.getInstance().processAttr(targetObj, targetObj.getClass());
//		}
//		
//	}
	
//	protected Statement processAttr(FrameworkMethod method, Object target,
//            Statement statement){
//		try {
//			Method processAttr = ProcessTagretObj.class.getMethod("processAttr");
//			FrameworkMethod processAttrMethod = new ProcessTagretObj(processAttr,target);
//			return new RunBefores(statement,Lists.newArrayList(processAttrMethod), processAttrMethod);
//        } catch (NoSuchMethodException | SecurityException e) {
//        	e.printStackTrace();
//        }
//		return statement;
//    }
	
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
