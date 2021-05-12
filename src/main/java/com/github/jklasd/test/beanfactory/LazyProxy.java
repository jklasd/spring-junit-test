 package com.github.jklasd.test.beanfactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopContextSuppert;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAttribute;

import com.github.jklasd.test.InvokeUtil;
import com.github.jklasd.test.LazyBeanProcess;
import com.github.jklasd.test.db.TranstionalManager;
import com.google.common.base.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class LazyProxy {
    protected BeanModel beanModel;
    protected Object tagertObj;
    protected volatile boolean inited;
    protected Object commonIntercept(Object poxy, Method method, Object[] param) throws Throwable {
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
            
            TransactionAttribute oldTxInfo = TranstionalManager.getInstance().getTxInfo();
            TransactionAttribute txInfo = TranstionalManager.getInstance().processAnnoInfo(method, newObj);
            
            TransactionStatus txStatus = openTransation(oldTxInfo, txInfo);
            
            Object result = method.invoke(newObj, param);
            
            closeTransation(oldTxInfo, txStatus);
            
            AopContextSuppert.setProxyObj(oldObj);
            return result;
        } catch (Exception e) {
            log.error("LazyCglib#intercept ERROR=>{}#{}==>Message:{}", beanModel.getBeanClassName(), method.getName(),
                e.getMessage());
            Throwable tmp = e;
            if (e.getCause() != null) {
                tmp = e.getCause();
            }
            throw tmp;
        }
    }
    
    protected Object getTagertObj() {
        if (tagertObj != null) {
            if (tagertObj.getClass().getSimpleName().contains("com.sun.proxy")) {
                log.warn("循环处理代理Bean问题=>{}", beanModel.getBeanClassName());
                if (tagertObj.getClass().getSimpleName().contains(beanModel.getTagClass().getSimpleName())) {
                    tagertObj = null;
                }
            } else {
                return tagertObj;
            }
        }
        Object tmp = getTagertObjectCustom();
        if(tmp!=null && !inited) {
            inited = true;
            if(beanModel.getAttr()!=null && beanModel.getAttr().size()>0) {
                beanModel.getAttr().forEach((key,value)->{
                    LazyBean.setAttr(key, tmp, beanModel.getTagClass(), value);
                });
            }
            if(beanModel.getBeanMethods()!=null) {
                beanModel.getBeanMethods().keySet().stream().filter(key -> Objects.equal(key, "init-method")).forEach(key -> {
                    InvokeUtil.invokeMethod(tagertObj, beanModel.getBeanMethods().get(key));
                });
            }
        }
        return tmp;
    }
    
    protected abstract Object getTagertObjectCustom();
    /**
     * 开启事务
     * @param oldTxInfo 旧事务信息
     * @param txInfo 新事务信息
     * @return
     */
    protected TransactionStatus openTransation(TransactionAttribute oldTxInfo, TransactionAttribute txInfo) {
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
        return txStatus;
    }
    /**
     * 关闭事务
     * @param oldTxInfo 旧事务信息
     * @param txStatus 当前事务
     */
    protected void closeTransation(TransactionAttribute oldTxInfo, TransactionStatus txStatus) {
        if (txStatus != null) {
            TranstionalManager.getInstance().commitTx(txStatus);
            TranstionalManager.getInstance().clearThradLocal();
        }
        if (oldTxInfo != null) {
            TranstionalManager.getInstance().setTxInfo(oldTxInfo);
        }
    }
}
