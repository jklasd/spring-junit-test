 package com.github.jklasd.test.beanfactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopContextSuppert;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAttribute;

import com.github.jklasd.test.InvokeUtil;
import com.github.jklasd.test.LazyBeanProcess;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.db.TranstionalManager;
import com.github.jklasd.test.spring.xml.XmlBeanUtil;
import com.google.common.base.Objects;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class LazyProxy {
    protected BeanModel beanModel;
    protected Object tagertObj;
    protected volatile boolean inited;
    @Getter
    private Map<String,Object> attr;
    public LazyProxy(BeanModel beanModel) {
        this.beanModel = beanModel;
        if(beanModel.getPropValue()!=null && beanModel.getPropValue().getPropertyValueList().size()>0) {
            attr = XmlBeanUtil.getInstance().handPropValue(beanModel.getPropValue().getPropertyValueList(), beanModel.getTagClass());
            XmlBeanUtil.getInstance().processValue(attr, beanModel.getTagClass());
            beanModel.setPropValue(null);
        }
    }

    protected void initLazyProxy() {
            try {
                if (ScanUtil.isImple(beanModel.getTagClass(), FactoryBean.class)) {
                    getTagertObj();
                    if(ScanUtil.isImple(beanModel.getTagClass(), InitializingBean.class)) {
                        InvokeUtil.invokeMethod(tagertObj, "afterPropertiesSet");
                    }
                    Class<?> tagC = (Class<?>)ScanUtil.getGenericType(beanModel.getTagClass())[0];
                    TestUtil.getApplicationContext().registBean(beanModel.getBeanName(), InvokeUtil.invokeMethod(tagertObj, "getObject"),
                        tagC);
                }
            } catch (Exception e) {
                 e.printStackTrace();
            }
    }
    
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
            log.error("LazyCglib#intercept ERROR=>{}#{}==>Message:{}", beanModel.getTagClass(), method.getName(),
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
                log.warn("循环处理代理Bean问题=>{}", beanModel.getTagClass());
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
            if(attr!=null && attr.size()>0) {
                attr.forEach((key,value)->{
                    LazyBean.setAttr(key, tmp, beanModel.getTagClass(), value);
                });
            }
            if(beanModel.getBeanMethods()!=null) {
                beanModel.getBeanMethods().keySet().stream().filter(key -> Objects.equal(key, "init-method")).forEach(key -> {
                    InvokeUtil.invokeMethod(tagertObj, beanModel.getBeanMethods().get(key));
                });
            }
            if(tagertObj instanceof InitializingBean) {
//                LazyBeanProcess.afterPropertiesSet(tagertObj);
                try {
                    InvokeUtil.invokeMethod(tagertObj, "afterPropertiesSet");
                } catch (SecurityException | IllegalArgumentException e) {
                    log.error("InitializingBean#afterPropertiesSet", e);
                }
            }
        }
        return tmp;
    }
    
    protected abstract Object getTagertObjectCustom();
    /**
     * 开启事务
     * @param oldTxInfo 旧事务信息
     * @param txInfo 新事务信息
     * @return 事务状态信息
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
