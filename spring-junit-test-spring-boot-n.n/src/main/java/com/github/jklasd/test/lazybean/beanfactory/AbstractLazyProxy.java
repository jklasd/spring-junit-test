 package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAttribute;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazybean.filter.LazyBeanFilter;
import com.github.jklasd.test.lazybean.model.BeanModel;
import com.github.jklasd.test.lazyplugn.db.TranstionalManager;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.github.jklasd.test.spring.suppert.AopContextSuppert;
import com.github.jklasd.test.util.BeanNameUtil;
import com.github.jklasd.test.util.JunitInvokeUtil;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractLazyProxy {
    @Getter
    protected BeanModel beanModel;
    protected Object tagertObj;
    protected volatile boolean inited;
    protected LazyApplicationContext applicationContext = LazyApplicationContext.getInstance();
    @Getter
    private Map<String,Object> attr;
    
    public AbstractLazyProxy(BeanModel beanModel) {
        this.beanModel = beanModel;
        if(beanModel.getPropValue()!=null && beanModel.getPropValue().getPropertyValueList().size()>0) {
            attr = XmlBeanUtil.getInstance().handPropValue(beanModel.getPropValue().getPropertyValueList(), beanModel.getTagClass());
            XmlBeanUtil.getInstance().processValue(attr, beanModel.getTagClass());
            beanModel.setPropValue(null);
        }
    }
    
    protected List factoryList;
    public static final String PROXY_BOUND = "CGLIB$BOUND";
    public static final String PROXY_CALLBACK_0 = "CGLIB$CALLBACK_0";
    public static boolean isProxy(Object obj){
    	try {
			Field bound = obj.getClass().getDeclaredField(PROXY_BOUND);
			return bound!=null;
		} catch (NoSuchFieldException | SecurityException e) {
			return false;
		}
    }
    
    public static Class<?> getProxyTagClass(Object obj){
    	try {
			if(isProxy(obj)) {
				Field bound = obj.getClass().getDeclaredField(PROXY_CALLBACK_0);
				bound.setAccessible(true);
				AbstractLazyProxy proxy = (AbstractLazyProxy) bound.get(obj);
				return proxy.getBeanModel().getTagClass();
			}
			return null;
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			return null;
		}
    }
    
    protected void initLazyProxy() {
            try {
                if (ScanUtil.isImple(beanModel.getTagClass(), FactoryBean.class)) {
                    log.info("initLazyProxy=>{}",beanModel.getTagClass());
                    getTagertObj();
                    Class<?> tagC = (Class<?>)ScanUtil.getGenericType(beanModel.getTagClass())[0];
                    Object obj = JunitInvokeUtil.invokeMethod(tagertObj, "getObject");
                    TestUtil.getInstance().getApplicationContext().registBean(BeanNameUtil.getBeanName(obj.getClass()), obj,
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
    
    private ThreadLocal<Map<String,Object>> lastInvoker = new ThreadLocal<Map<String,Object>>();
    
//    private AtomicInteger buildObjTimes = new AtomicInteger();
    private AtomicInteger errorTimes = new AtomicInteger();
    
    protected  Object commonIntercept(Object poxy, Method method, Object[] param) throws Throwable {
    	if(errorTimes.get()>3) {
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
    	Map<String,Object> lastInvokerInfo = lastInvoker.get();
        try {
        	Map<String,Object> tmpInvokerInfo = Maps.newHashMap();
        	tmpInvokerInfo.put("class", beanModel.getTagClass());
        	tmpInvokerInfo.put("method", method.getName());
        	lastInvoker.set(tmpInvokerInfo);
            Object oldObj = AopContextSuppert.getProxyObject();

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

            LazyBeanFilter.processLazyConfig(newObj, method, param);
            
            Object result = null;
            if(TranstionalManager.isFindTranstional()) {
            	TransactionAttribute oldTxInfo = TranstionalManager.getInstance().getTxInfo();
            	TransactionAttribute txInfo = TranstionalManager.getInstance().processAnnoInfo(method, newObj);
            	
            	TransactionStatus txStatus = openTransation(oldTxInfo, txInfo);
            	
            	result = method.invoke(newObj, param);
            	
            	closeTransation(oldTxInfo, txStatus);
            }else {
            	result = method.invoke(newObj, param);
            }
            
            errorTimes.set(0);
            
            AopContextSuppert.setProxyObj(oldObj);
            lastInvoker.set(lastInvokerInfo);
            return result;
        }catch (JunitException e) {
        	log.warn("LazyCglib#intercept warn.lastInvoker=>{}", lastInvokerInfo);
        	throw e;
        }catch (InvocationTargetException e) {
//        	log.warn("===================InvocationTargetException处理===================");
        	throw e.getTargetException();
		}catch (Exception e) {
        	errorTimes.incrementAndGet();
        	log.warn("LazyCglib#intercept warn.lastInvoker=>{}", lastInvokerInfo);
            log.error("LazyCglib#intercept ERROR=>{}#{}==>message:{},params:{}", beanModel.getTagClass(), method.getName(),
                e.getMessage());
            throw e;
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
        log.debug("开始实例化:{}",beanModel);
//        if(beanModel.getTagClass().getName().contains("mongodb.MongoClient")) {
//        	log.debug("断点");
//        }
        Object tmp = getTagertObjectCustom();
        if(tmp!=null && !inited) {
            inited = true;
            if(attr!=null && attr.size()>0) {
                attr.forEach((key,value)->{
                    LazyBean.getInstance().setAttr(key, tmp, beanModel.getTagClass(), value);
                });
            }
            if(beanModel.getBeanMethods()!=null) {
                beanModel.getBeanMethods().keySet().stream().filter(key -> Objects.equal(key, "init-method")).forEach(key -> {
                    JunitInvokeUtil.invokeMethod(tagertObj, beanModel.getBeanMethods().get(key));
                });
            }
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

	public static void instantiateProxy(Object obj) {
		try {
			Field bound = obj.getClass().getDeclaredField(PROXY_CALLBACK_0);
			bound.setAccessible(true);
			AbstractLazyProxy proxy = (AbstractLazyProxy) bound.get(obj);
			proxy.getTagertObjectCustom();
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
		}
	}
}
