 package com.github.jklasd.test.lazyplugn.db;

import java.lang.reflect.Method;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyProxyManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TranstionalManager {
    private static TranstionalManager transtionManager;
    private volatile PlatformTransactionManager txManager;
    private TranstionalManager() {}
    private static Class<?> DataSourceTransactionManager = ScanUtil.loadClass("org.springframework.jdbc.datasource.DataSourceTransactionManager");
    public static boolean isFindTranstional() {
    	return DataSourceTransactionManager != null;
    }
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
    public TransactionAttribute processAnnoInfo(Method method,Object obj){
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
            txManager = (DataSourceTransactionManager) LazyProxyManager.instantiateProxy(txManager);
        }
        //final方法处理
        return txManager.getTransaction(txInfo);//不能是代理类
    }
    
    public void rollbackTx(TransactionStatus txStatus) {
    	txManager.rollback(txStatus);
    	log.info("回滚事务=>{}",txStatus);
    }
    
    public void commitTx(TransactionStatus txStatus) {
        txManager.commit(txStatus);
        log.info("提交事务=>{}",txStatus);
    }
}
