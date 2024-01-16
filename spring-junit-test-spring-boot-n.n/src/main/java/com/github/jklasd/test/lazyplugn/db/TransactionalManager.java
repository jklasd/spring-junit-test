 package com.github.jklasd.test.lazyplugn.db;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyProxyManager;
import com.github.jklasd.test.spring.suppert.JunitDataSourceBuild;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionalManager {
    private static TransactionalManager transtionManager;
    private volatile DataSourceTransactionManager txManager;
    private TransactionalManager() {}
    private static Class<?> DataSourceTransactionManager = ScanUtil.loadClass("org.springframework.jdbc.datasource.DataSourceTransactionManager");
    public static boolean isFindTranstional() {
    	return DataSourceTransactionManager != null;
    }
    public synchronized static TransactionalManager getInstance() {
        if(transtionManager == null) {
            transtionManager = new TransactionalManager();
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
        if(txManager == null) {
            txManager = TestUtil.getInstance().getApplicationContext().getBean(DataSourceTransactionManager.class);
            txManager = (DataSourceTransactionManager) LazyProxyManager.instantiateProxy(txManager);
            //junit 工具需要控制多数据源，所以需要对事务管理器重置数据源。
            txManager.setDataSource(JunitDataSourceBuild.rebuildDataSource(txManager.getDataSource()));
        }
        log.info("开启事务=>{}",txInfo);
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
