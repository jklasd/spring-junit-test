package com.github.jklasd.test.spring.suppert;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.aop.framework.AopContext;

import com.github.jklasd.test.core.facade.JunitClassLoader;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.util.ScanUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang AopContext 工具类
 */
@Slf4j
public class AopContextSuppert{
	static Class<?> AopContextClass = ScanUtil.loadClass("org.springframework.aop.framework.AopContext");
    private static Object aopContext;
    private static Method setCurrentProxy;
    /**
     * 提前注册，防止applicationContext.getBean(class)执行时，会对所有bean类型进行比对时
     * 调用DefaultListableBeanFactory#doGetBeanNamesForType
     * // Check manually registered singletons too.
     * //for (String beanName : this.manualSingletonNames) {
     * 	……
     * }
     * 
     * 这时触发部分FactoryBean#getObjectType方法。又再添加新Bean时会修改manualSingletonNames内容。导致遍历对象变动产生异常
     * 
     */
    public static void registerObj() {
    	if(AopContextClass == null) {
    		return;
    	}
    	aopContext = LazyBean.getInstance().buildProxy(AopContextClass);
    }
    public static void setProxyObj(Object obj) {
    	if(AopContextClass == null) {
    		return;
    	}
        try {
            if (aopContext == null) {
                /**
                 * 兼容AopContext 升级处理
                 */
                if(Modifier.isAbstract(AopContextClass.getModifiers())
                		&& !Modifier.isFinal(AopContextClass.getModifiers())) {
                    //抽象类处理
                    aopContext = LazyBean.getInstance().buildProxy(AopContext.class);
                }else {
                    //非抽象类处理
                    aopContext = LazyBean.getInstance().invokeBuildObject(AopContextClass);
                }
            }
            if(setCurrentProxy == null) {
            	setCurrentProxy = AopContext.class.getDeclaredMethod("setCurrentProxy", Object.class);
            	if(!setCurrentProxy.isAccessible()) {
            		setCurrentProxy.setAccessible(true);
            	}
            }
            setCurrentProxy.invoke(aopContext, obj);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
             log.error("AopContextSuppert#setProxyObj",e);
        }
//        InvokeUtil.invokeStaticMethod(AopContext.class, "setCurrentProxy",
//            MethodType.methodType(Object.class, Object.class), obj);
    }
	public static Object getProxyObject() {
		if(AopContextClass == null) {
    		return null;
    	}
		try {
			return AopContext.currentProxy();
		} catch (Exception e) {
		}
		return null;
	}
}
