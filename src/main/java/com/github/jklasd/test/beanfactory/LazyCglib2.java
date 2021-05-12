package com.github.jklasd.test.beanfactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopContextSuppert;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAttribute;

import com.github.jklasd.test.LazyBeanProcess;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.db.TranstionalManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyCglib2 implements MethodInterceptor {
    @Getter
    private Class<?> tagertC;
    private BeanDefinition beanDef;
    private Object tagertObj;
    @Getter
    private Constructor<?> constructor;
    
    public LazyCglib2(Class<?> tagertC) {
        this.tagertC = tagertC;
        setConstructor();
    }
    
    public LazyCglib2(BeanDefinition beanDef) {
        this.beanDef = beanDef;
        this.tagertC = ScanUtil.loadClass(beanDef.getBeanClassName());
        setConstructor();
    }

    public boolean isFinal() {
        Method[] ms = tagertC.getDeclaredMethods();
        for(Method m : ms) {
            if(Modifier.isFinal(m.getModifiers())
                    && Modifier.isPublic(m.getModifiers())) {
                //存在final方法，且是公共方法，不能使用代理对象
                return true;
            }
        }
        return false;
    }

    /**
     * 确定构造器
     */
    private void setConstructor() {
        Constructor<?>[] structors = tagertC.getConstructors();
        int count = 10;
        if(structors.length <1) {
            structors = tagertC.getDeclaredConstructors();
            for(Constructor<?> c : structors) {
//                Class<?> tagC = c.getDeclaringClass();
                if(c.getParameterCount()<count) {
                    this.constructor = c;
                    count = c.getParameterCount();
                }
            }
            constructor.setAccessible(true);
        }else {
            for(Constructor<?> c : structors) {
//                Class<?> tagC = c.getDeclaringClass();
                if(c.getParameterCount()<count) {
                    this.constructor = c;
                    count = c.getParameterCount();
                }
            }
        }
    }

    public Object[] getArguments() {
        Object[] objes = new Object[constructor.getParameters().length];
        for(int i=0;i<objes.length;i++) {
            Class<?> c = getArgumentTypes()[i];
            if(c == String.class) {
                objes[i] = "";
            }else if(c == Integer.class || c == int.class){
                objes[i] = 0;
            }else if(c == Double.class || c == double.class){
                objes[i] = (double)0;
            }else if(c == Byte.class || c == byte.class){
                objes[i] = (byte)0;
            }else if(c == Long.class || c == long.class){
                objes[i] = 0l;
            }else if(c == Boolean.class || c == boolean.class){
                objes[i] = false;
            }else if(c == Float.class || c == float.class){
                objes[i] = 0.0;
            }else if(c == Short.class || c == short.class ){
                objes[i] = 0;
            }else if(c == char.class){
                objes[i] = '0';
            }else if(c.getName().contains("java.util.List")) {
                objes[i] = Lists.newArrayList();
            }else if(c.getName().contains("java.util.Set")) {
                objes[i] = Sets.newHashSet();
            }
            else {
                objes[i] = LazyBean.buildProxy(getArgumentTypes()[i]);
            }
        }
        return objes;
    }
    public Class<?>[] getArgumentTypes() {
        return constructor.getParameterTypes();
    }
    
    @Override
    public Object intercept(Object poxy, Method method, Object[] param, MethodProxy arg3) throws Throwable {

        try {
            Object oldObj = null;
            try {
                oldObj = AopContext.currentProxy();
            } catch (IllegalStateException e) {
            }

            Object newObj = getTagertObj();

            if (!Modifier.isPublic(method.getModifiers())) {
                // log.warn("非公共方法 class:{},method:{}",tag,method.getName());
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                if (newObj != null) {
                    return method.invoke(newObj, param);
                }
                return null;
            }
            AopContextSuppert.setProxyObj(poxy);

            LazyBeanProcess.processLazyConfig(newObj, method, param);
            TranstionalManager.getInstance().processAnnoInfo(method, newObj);
            TransactionAttribute oldTxInfo = TranstionalManager.getInstance().getTxInfo();
            TransactionAttribute txInfo = TranstionalManager.getInstance().processAnnoInfo(method, newObj);
            TransactionStatus txStatus = null;
            if (txInfo != null) {
                TranstionalManager.getInstance().setTxInfo(txInfo);
                if (oldTxInfo != null) {
                    // 看情况开启新事务
                    if (txInfo.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED
                        || txInfo.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
                        txStatus = TranstionalManager.getInstance().beginTx(txInfo);
                    }
                } else {
                    // 开启事务
                    txStatus = TranstionalManager.getInstance().beginTx(txInfo);
                }
            }
            Object result = method.invoke(newObj, param);
            if (txStatus != null) {
                TranstionalManager.getInstance().commitTx(txStatus);
                TranstionalManager.getInstance().clearThradLocal();
            }
            if (oldTxInfo != null) {
                TranstionalManager.getInstance().setTxInfo(oldTxInfo);
            }
            TranstionalManager.getInstance().processAnnoInfo(method, newObj);
            AopContextSuppert.setProxyObj(oldObj);
            return result;
        } catch (Exception e) {
            log.error("LazyCglib#intercept ERROR=>{}#{}==>Message:{}", beanDef.getBeanClassName(), method.getName(),
                e.getMessage());
            Throwable tmp = e;
            if (e.getCause() != null) {
                tmp = e.getCause();
            }
            throw tmp;
        }

    }

    private Object getTagertObj() {
        if (tagertObj != null) {
            if (tagertObj.getClass().getSimpleName().contains("com.sun.proxy")) {
                log.warn("循环处理代理Bean问题=>{}", beanDef.getBeanClassName());
                if (tagertObj.getClass().getSimpleName().contains(tagertC.getSimpleName())) {
                    tagertObj = null;
                }
            } else {
                return tagertObj;
            }
        }

        
        
        return null;
    }
}