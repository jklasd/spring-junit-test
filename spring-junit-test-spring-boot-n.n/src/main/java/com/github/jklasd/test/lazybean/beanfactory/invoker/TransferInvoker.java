package com.github.jklasd.test.lazybean.beanfactory.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAttribute;

import com.github.jklasd.test.lazyplugn.db.TranstionalManager;

public class TransferInvoker extends AbstractProxyInvoker{
	
	private static TransferInvoker instance;
	public static TransferInvoker getInstance() {
		if(instance == null) {
			synchronized (TransferInvoker.class) {
				if(instance == null) {
					instance = new TransferInvoker();
				}
			}
		}
		return instance;
	}

	private final String context_oldTxInfo = "oldTxInfo";
	private final String context_txStatus = "txStatus";
	/*
	 * TransactionAttribute oldTxInfo = null;
    	TransactionStatus txStatus = null;
        try {
            Object newObj = getTagertObj();
            Object result = null;
            if(TranstionalManager.isFindTranstional()) {
            	oldTxInfo = TranstionalManager.getInstance().getTxInfo();
            	TransactionAttribute txInfo = TranstionalManager.getInstance().processAnnoInfo(method, newObj);
            	
            	txStatus = openTransation(oldTxInfo, txInfo);
            	
            	result = method.invoke(newObj, param);
            	closeTransation(oldTxInfo, txStatus,method);
            }else {
            	result = method.invoke(newObj, param);
            }
            
            return result;
        }catch (JunitException e) {
        	rollback(oldTxInfo, txStatus,e);
        	throw e;
        }catch (InvocationTargetException e) {
        	rollback(oldTxInfo, txStatus,e);
        	throw e.getTargetException();
		}catch (Exception e) {
			/**
			 * 抛出异常，一定要关闭事务
			 * 否则在批量测试中，会导致其他单元测试，进入事务。
			 */
//			rollback(oldTxInfo, txStatus,e);
//            throw e;
//        }
	
	/**
	 * 正常关闭事务
	 */
	@Override
	protected void afterInvoke(InvokeDTO dto, Map<String, Object> context) {
		if(!TranstionalManager.isFindTranstional()) {
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
		if(!TranstionalManager.isFindTranstional()) {
			return true;
		}
		TransactionAttribute oldTxInfo = null;
		TransactionStatus txStatus = null;
		
		Object newObj = dto.getRealObj();
    	oldTxInfo = TranstionalManager.getInstance().getTxInfo();
    	context.put(context_oldTxInfo,oldTxInfo);	
    	TransactionAttribute txInfo = TranstionalManager.getInstance().processAnnoInfo(dto.getMethod(), newObj);
    	txStatus = openTransation(oldTxInfo, txInfo);
    	context.put(context_txStatus,txStatus);	
    	return true;
	}

	/**
	 * 异常回滚
	 */
	@Override
	protected void exceptionInvoke(InvokeDTO dto, Map<String, Object> context, Exception e) {
		if(!TranstionalManager.isFindTranstional()) {
			return ;
		}
		TransactionAttribute oldTxInfo = (TransactionAttribute) context.get(context_oldTxInfo);	
		TransactionStatus txStatus = (TransactionStatus) context.get(context_txStatus);
		rollback(oldTxInfo, txStatus, e);
	}
	
	/**
	 * 回滚事务
	 * @param oldTxInfo
	 * @param txStatus
	 * @param e
	 */
	
	protected void rollback(TransactionAttribute oldTxInfo, TransactionStatus txStatus, Exception e) {
		
    	if(txStatus!=null) {
    		TransactionAttribute currentTxInfo = TranstionalManager.getInstance().getTxInfo();
    		if(currentTxInfo.rollbackOn(e)) {
    			TranstionalManager.getInstance().rollbackTx(txStatus);
    		}else {
    			TranstionalManager.getInstance().commitTx(txStatus);
    		}
    		TranstionalManager.getInstance().clearThradLocal();
    	}
    	if (oldTxInfo != null) {
            TranstionalManager.getInstance().setTxInfo(oldTxInfo);
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
     * @param method 
     */
    protected void closeTransation(TransactionAttribute oldTxInfo, TransactionStatus txStatus, Method method) {
        if (txStatus != null) {
            TranstionalManager.getInstance().commitTx(txStatus);
            TranstionalManager.getInstance().clearThradLocal();
        }
        if (oldTxInfo != null) {
            TranstionalManager.getInstance().setTxInfo(oldTxInfo);
        }
        
    }
}
