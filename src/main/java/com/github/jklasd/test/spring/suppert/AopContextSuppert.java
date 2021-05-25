package com.github.jklasd.test.spring.suppert;

import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.aop.framework.AopContext;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang AopContext 工具类
 */
@Slf4j
public class AopContextSuppert extends AopContext {
    private static AopContextSuppert aopContext;
    private static Method setCurrentProxy;
    public static void setProxyObj(Object obj) {
        try {
            if (aopContext == null) {
                aopContext = new AopContextSuppert();
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
}
