 package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.interf.handler.MockFieldHandlerI;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.github.jklasd.test.util.BeanNameUtil;
import com.github.jklasd.test.util.JunitInvokeUtil;
import com.google.common.base.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class AbstractLazyProxy extends BaseAbstractLazyProxy{
    
    protected Object tagertObj;
    protected volatile boolean inited;
    protected LazyApplicationContext applicationContext = LazyApplicationContext.getInstance();
    
    
    public AbstractLazyProxy(BeanModel beanModel) {
        this.beanModel = beanModel;
        if(beanModel.getPropValue()!=null && beanModel.getPropValue().getPropertyValueList().size()>0) {
            attr = XmlBeanUtil.getInstance().handPropValue(beanModel.getPropValue().getPropertyValueList(), beanModel.getTagClass());
            XmlBeanUtil.getInstance().processValue(attr, beanModel.getTagClass());
            beanModel.setPropValue(null);
        }
    }
    
    private MockFieldHandlerI handler = ContainerManager.getComponent(ContainerManager.NameConstants.MockFieldHandler);
    
//    @Deprecated
//    public static boolean isProxy(Object obj){
//		return LazyProxyManager.isProxy(obj);
//    }
//    @Deprecated
//    public static Object getProxyTagObj(Object obj){
//    	return LazyProxyManager.getProxyTagObj(obj);
//    }
//    @Deprecated
//    public static Class<?> getProxyTagClass(Object obj){
//    	return LazyProxyManager.getProxyTagClass(obj);
//    }
    
    protected void initLazyProxy() {
            try {
                if (ScanUtil.isImple(beanModel.getTagClass(), FactoryBean.class)) {
                    log.info("initLazyProxy=>{}",beanModel.getTagClass());
                    getTagertObj();
                    Class<?> tagC = (Class<?>)ScanUtil.getGenericType(beanModel.getTagClass())[0];
                    Object obj = JunitInvokeUtil.invokeMethod(tagertObj, "getObject");
                    TestUtil.getInstance().getApplicationContext().registProxyBean(BeanNameUtil.getBeanName(obj.getClass()), obj,
                        tagC);
                }
            } catch (Exception e) {
                 if(beanModel.isThrows()) {
                	 throw e;
                 }else {
                	 e.printStackTrace();
                 }
            }
    }
    
    private AtomicInteger errorTimes = new AtomicInteger();
    
//    @Deprecated
//    private Object transtionalHandler(Object poxy, Method method, Object[] param) throws Throwable{
//    	TransactionAttribute oldTxInfo = null;
//    	TransactionStatus txStatus = null;
//        try {
//            Object newObj = getTagertObj();
//            Object result = null;
//            if(TranstionalManager.isFindTranstional()) {
//            	oldTxInfo = TranstionalManager.getInstance().getTxInfo();
//            	TransactionAttribute txInfo = TranstionalManager.getInstance().processAnnoInfo(method, newObj);
//            	
//            	txStatus = openTransation(oldTxInfo, txInfo);
//            	
//            	result = method.invoke(newObj, param);
//            	closeTransation(oldTxInfo, txStatus,method);
//            }else {
//            	result = method.invoke(newObj, param);
//            }
//            
//            return result;
//        }catch (JunitException e) {
//        	rollback(oldTxInfo, txStatus,e);
//        	throw e;
//        }catch (InvocationTargetException e) {
//        	rollback(oldTxInfo, txStatus,e);
//        	throw e.getTargetException();
//		}catch (Exception e) {
//			/**
//			 * 抛出异常，一定要关闭事务
//			 * 否则在批量测试中，会导致其他单元测试，进入事务。
//			 */
//			rollback(oldTxInfo, txStatus,e);
//            throw e;
//        }
//    }
//    
//    @Deprecated
//	private Object aopHandler(Object poxy, Method method, Object[] param) throws Throwable{
//    	Map<String,Object> lastInvokerInfo = LazyProxyManager.getLastInvoker();
//    	Object oldObj = AopContextSuppert.getProxyObject();
//        try {
//        	
//        	LazyProxyManager.setLastInvoker(beanModel.getTagClass(), method);
//
//            Object newObj = getTagertObj();
//
//            AopContextSuppert.setProxyObj(poxy);
//
//            LazyBeanFilter.processLazyConfig(newObj, method, param);
//            
//            Object result = transtionalHandler(poxy, method, param);
//            
//            LazyProxyManager.setLastInvoker(lastInvokerInfo);
//            return result;
//        }catch (JunitException e) {
//        	log.warn("LazyCglib#intercept warn.lastInvoker=>{}", lastInvokerInfo);
//        	throw e;
//        }catch (InvocationTargetException e) {
//        	throw e.getTargetException();
//		}catch (Exception e) {
//        	log.warn("LazyCglib#intercept warn.lastInvoker=>{}", lastInvokerInfo);
//            log.error("LazyCglib#intercept ERROR=>{}#{}==>message:{},params:{}", beanModel.getTagClass(), method.getName(),
//                e.getMessage());
//            throw e;
//        }finally {
//        	AopContextSuppert.setProxyObj(oldObj);
//		}
//    }
//    
    protected  Object commonIntercept(Object poxy, Method method, Object[] param) throws Throwable {
    	if(errorTimes.get()>6) {
    		log.error("Class=>{},method=>{}",tagertObj.getClass(),method.getName());
    		throw new JunitException("----------异常代理方式--------",true);
    	}
    	if(method.getName().equals("toString")) {
    		if(tagertObj!=null) {
    			return tagertObj.toString();
    		}else {
    			return this.toString();
    		}
    	}else if(method.getName().equals("hashCode")) {
    		if(tagertObj!=null) {
    			return tagertObj.hashCode();
    		}else {
    			return this.hashCode();
    		}
    	}
    	log.debug("exec stack=>{}",method);
		try {
			if (!Modifier.isPublic(method.getModifiers())) {
                // log.warn("非公共方法 class:{},method:{}",tag,method.getName());
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
            }
		//	injeckMock拦截
			Object obj = null;
			if(handler!= null && handler.finded(beanModel)) {
				obj = handler.invoke(poxy, method, param,beanModel);
			}else {
//				obj = aopHandler(poxy, method, param);
				obj = LazyProxyManager.getProxyInvoker().invoke(poxy, method, param, beanModel, getTagertObj());
			}
			errorTimes.set(0);
			return obj;
        }catch (JunitException e) {
        	errorTimes.incrementAndGet();
        	throw e;
        }catch (InvocationTargetException e) {
        	errorTimes.incrementAndGet();
        	throw e.getTargetException();
		}catch (Exception e) {
        	errorTimes.incrementAndGet();
            throw e;
        }
    }
    
	protected Object getTagertObj() {
        if (tagertObj != null && inited) {//需初始化完成，才返回，否则存在线程安全问题
            if (tagertObj.getClass().getSimpleName().contains("com.sun.proxy")) {
                log.warn("循环处理代理Bean问题=>{}", beanModel.getTagClass());
                if (tagertObj.getClass().getSimpleName().contains(beanModel.getTagClass().getSimpleName())) {
                    tagertObj = null;
                }
            } else {
                return tagertObj;
            }
        }
        log.debug("开始实例化:{}",beanModel);
        Object tmp = getTagertObjectCustom();
        if(tmp!=null && !inited) {
            inited = true;
            if(attr!=null && attr.size()>0) {
                attr.forEach((key,value)->{
                    LazyBean.getInstance().setAttr(key, tmp, beanModel.getTagClass(), value);
                });
            }
//            if(beanModel.getBeanMethods()!=null) {
//                beanModel.getBeanMethods().keySet().stream().filter(key -> Objects.equal(key, "init-method")).forEach(key -> {
//                    JunitInvokeUtil.invokeMethod(tagertObj, beanModel.getBeanMethods().get(key));
//                });
//            }
            if(tagertObj instanceof InitializingBean) {
                try {
                    JunitInvokeUtil.invokeMethod(tagertObj, "afterPropertiesSet");
                } catch (SecurityException | IllegalArgumentException e) {
                    log.error("InitializingBean#afterPropertiesSet", e);
                    if(beanModel.isThrows()) {
                    	throw e;
                    }
                }
            }
        }
        return tmp;
    }
    
    protected abstract Object getTagertObjectCustom();
//    /**
//     * 开启事务
//     * @param oldTxInfo 旧事务信息
//     * @param txInfo 新事务信息
//     * @return 事务状态信息
//     */
//    @Deprecated
//    protected TransactionStatus openTransation(TransactionAttribute oldTxInfo, TransactionAttribute txInfo) {
//        TransactionStatus txStatus = null;
//        if (txInfo != null) {
//            TranstionalManager.getInstance().setTxInfo(txInfo);
//            if (oldTxInfo != null) {
//                // 看情况开启新事务
//                if (txInfo.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED
//                    || txInfo.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
//                    txStatus = TranstionalManager.getInstance().beginTx(txInfo);
//                }
//            } else {
//                // 开启事务
//                txStatus = TranstionalManager.getInstance().beginTx(txInfo);
//            }
//        }
//        return txStatus;
//    }
//    /**
//     * 关闭事务
//     * @param oldTxInfo 旧事务信息
//     * @param txStatus 当前事务
//     * @param method 
//     */
//    @Deprecated
//    protected void closeTransation(TransactionAttribute oldTxInfo, TransactionStatus txStatus, Method method) {
//        if (txStatus != null) {
//            TranstionalManager.getInstance().commitTx(txStatus);
//            TranstionalManager.getInstance().clearThradLocal();
//        }
//        if (oldTxInfo != null) {
//            TranstionalManager.getInstance().setTxInfo(oldTxInfo);
//        }
//        
//    }
//    @Deprecated
//    protected void rollback(TransactionAttribute oldTxInfo, TransactionStatus txStatus, Exception e) {
//    	if(txStatus!=null) {
//    		TransactionAttribute currentTxInfo = TranstionalManager.getInstance().getTxInfo();
//    		if(currentTxInfo.rollbackOn(e)) {
//    			TranstionalManager.getInstance().rollbackTx(txStatus);
//    		}else {
//    			TranstionalManager.getInstance().commitTx(txStatus);
//    		}
//    		TranstionalManager.getInstance().clearThradLocal();
//    	}
//    	if (oldTxInfo != null) {
//            TranstionalManager.getInstance().setTxInfo(oldTxInfo);
//        }
//    }
//    @Deprecated
//	public static Object instantiateProxy(Object obj) {
//		return LazyProxyManager.instantiateProxy(obj);
//	}
//    @Deprecated
//	public static BaseAbstractLazyProxy getProxy(Object obj) {
//		return LazyProxyManager.getProxy(obj);
//	}

}
