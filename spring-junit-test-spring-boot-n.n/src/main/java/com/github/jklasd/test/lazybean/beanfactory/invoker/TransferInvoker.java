package com.github.jklasd.test.lazybean.beanfactory.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAttribute;

import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazyplugn.db.TransactionalManager;

public class TransferInvoker extends AbstractProxyInvoker{
	private static Class<?> PlatformTransactionManager = ScanUtil.loadClass("org.springframework.transaction.PlatformTransactionManager");
	private static TransferInvoker instance;
	public static TransferInvoker getInstance() {
		if(instance == null) {
			if(PlatformTransactionManager!=null) {
				synchronized (TransferInvoker.class) {
					if(instance == null) {
						instance = new TransferInvoker();
					}
				}
			}
		}
		return instance;
	}

	private final String context_oldTxInfo = "oldTxInfo";
	private final String context_txStatus = "txStatus";
	
	/**
	 * 正常关闭事务
	 */
	@Override
	protected void afterInvoke(InvokeDTO dto, Map<String, Object> context) {
		if(!TransactionalManager.isFindTranstional()) {
			return ;
		}
		TransactionAttribute oldTxInfo = (TransactionAttribute) context.get(context_oldTxInfo);	
		TransactionStatus txStatus = (TransactionStatus) context.get(context_txStatus);
		closeTransation(oldTxInfo, txStatus,dto.getMethod());		
	}

	@Override
	protected void finallyInvoke(InvokeDTO dto, Map<String, Object> context) {
		
	}

	/**
	 * 开启事务
	 * 新旧事务交替
	 */
	@Override
	protected boolean beforeInvoke(InvokeDTO dto, Map<String, Object> context)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(!TransactionalManager.isFindTranstional()) {
			return true;
		}
		TransactionAttribute oldTxInfo = null;
		TransactionStatus txStatus = null;
		
		Object newObj = dto.getRealObj();
    	oldTxInfo = TransactionalManager.getInstance().getTxInfo();
    	context.put(context_oldTxInfo,oldTxInfo);	
    	TransactionAttribute txInfo = TransactionalManager.getInstance().processAnnoInfo(dto.getMethod(), newObj);
    	txStatus = openTransation(oldTxInfo, txInfo);
    	context.put(context_txStatus,txStatus);	
    	return true;
	}

	/**
	 * 异常回滚
	 */
	@Override
	protected void exceptionInvoke(InvokeDTO dto, Map<String, Object> context, Exception e) {
		if(!TransactionalManager.isFindTranstional()) {
			return ;
		}
		TransactionAttribute oldTxInfo = (TransactionAttribute) context.get(context_oldTxInfo);	
		TransactionStatus txStatus = (TransactionStatus) context.get(context_txStatus);
		rollback(oldTxInfo, txStatus, e);
	}
	
	/**
	 * 回滚事务
	 * @param oldTxInfo 旧事务信息
     * @param txStatus 当前事务状态
	 * @param e 导致回滚事务的异常
	 */
	
	protected void rollback(TransactionAttribute oldTxInfo, TransactionStatus txStatus, Exception e) {
		
    	if(txStatus!=null) {
    		TransactionAttribute currentTxInfo = TransactionalManager.getInstance().getTxInfo();
    		if(currentTxInfo.rollbackOn(e)) {
    			TransactionalManager.getInstance().rollbackTx(txStatus);
    		}else {
    			TransactionalManager.getInstance().commitTx(txStatus);
    		}
    		TransactionalManager.getInstance().clearThradLocal();
    	}
    	if (oldTxInfo != null) {
            TransactionalManager.getInstance().setTxInfo(oldTxInfo);
        }
    }
	
	/**
     * 开启事务
     * @param oldTxInfo 旧事务信息
     * @param txInfo 新事务信息
     * @return 事务状态信息
     */
    protected TransactionStatus openTransation(TransactionAttribute oldTxInfo, TransactionAttribute txInfo) {
        TransactionStatus txStatus = null;
        if (txInfo != null) {
            TransactionalManager.getInstance().setTxInfo(txInfo);
            if (oldTxInfo != null) {
                // 看情况开启新事务
//                if (txInfo.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED
//                    || txInfo.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
            	if (txInfo.getPropagationBehavior() > TransactionDefinition.PROPAGATION_REQUIRED) {
                    txStatus = TransactionalManager.getInstance().beginTx(txInfo);
                }
            } else {
                // 开启事务
                txStatus = TransactionalManager.getInstance().beginTx(txInfo);
            }
        }
        return txStatus;
    }
    /**
     * 
     * 关闭事务
     * @param oldTxInfo 旧事务信息
     * @param txStatus 当前事务状态
     * @param method 存在事务的方法 
     */
    protected void closeTransation(TransactionAttribute oldTxInfo, TransactionStatus txStatus, Method method) {
        if (txStatus != null) {
            TransactionalManager.getInstance().commitTx(txStatus);
            TransactionalManager.getInstance().clearThradLocal();
        }
        if (oldTxInfo != null) {
            TransactionalManager.getInstance().setTxInfo(oldTxInfo);
        }
        
    }
}
