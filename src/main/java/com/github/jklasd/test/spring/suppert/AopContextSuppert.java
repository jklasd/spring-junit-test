package com.github.jklasd.test.spring.suppert;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.aop.framework.AopContext;

import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang AopContext 工具类
 */
@Slf4j
public class AopContextSuppert{
    private static Object aopContext;
    private static Method setCurrentProxy;
    public static void setProxyObj(Object obj) {
        try {
            if (aopContext == null) {
                /**
                 * 兼容AopContext 升级处理
                 */
                if(Modifier.isAbstract(AopContext.class.getModifiers())) {
                    //抽象类处理
                    aopContext = LazyBean.getInstance().buildProxy(AopContext.class);
                }else {
                    //非抽象类处理
                    Constructor<?>[] cons = AopContext.class.getDeclaredConstructors();
                    if(!cons[0].isAccessible()) {
                        cons[0].setAccessible(true);
                    }
                    aopContext = cons[0].newInstance();
                }
                setCurrentProxy = AopContext.class.getDeclaredMethod("setCurrentProxy", Object.class);
                if(!setCurrentProxy.isAccessible()) {
                    setCurrentProxy.setAccessible(true);
                }
            }
            setCurrentProxy.invoke(aopContext, obj);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException e) {
             log.error("AopContextSuppert#setProxyObj",e);
        }
//        InvokeUtil.invokeStaticMethod(AopContext.class, "setCurrentProxy",
//            MethodType.methodType(Object.class, Object.class), obj);
    }
}
