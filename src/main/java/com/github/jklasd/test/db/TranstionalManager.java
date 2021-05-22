 package com.github.jklasd.test.db;

import java.lang.reflect.Method;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

import com.github.jklasd.test.TestUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TranstionalManager {
    private static TranstionalManager transtionManager;
    private volatile PlatformTransactionManager txManager;
    private TranstionalManager() {}
    public synchronized static TranstionalManager getInstance() {
        if(transtionManager == null) {
            transtionManager = new TranstionalManager();
        }
        return transtionManager;
    }
    private ThreadLocal<TransactionAttribute> provTxInfo = new ThreadLocal<>();
    
    public void setTxInfo(TransactionAttribute info) {
        provTxInfo.set(info);
    }
    
    public TransactionAttribute getTxInfo() {
        return provTxInfo.get();
    }
    
    public void clearThradLocal() {
        provTxInfo.remove();
    }
    
    private AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    public TransactionAttribute processAnnoInfo(Method method,Object obj) throws Exception {
        if(obj == null) {
            return null;
        }
        Class<?> tagClass = obj.getClass();
        if(tagClass == DataSourceTransactionManager.class) {
            return null;
        }
        return atas.getTransactionAttribute(method, tagClass);
    }
    public TransactionStatus beginTx(TransactionAttribute txInfo) {
        log.info("开启事务=>{}",txInfo);
        if(txManager == null) {
            txManager = TestUtil.getInstance().getApplicationContext().getBean(DataSourceTransactionManager.class);
        }
        return txManager.getTransaction(txInfo);
    }
    
    public void hangTx() {
//        txManager.
    }
    
    public void commitTx(TransactionStatus txStatus) {
        txManager.commit(txStatus);
    }
}
