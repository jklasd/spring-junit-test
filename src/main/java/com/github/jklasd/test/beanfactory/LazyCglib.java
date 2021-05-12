package com.github.jklasd.test.beanfactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopContextSuppert;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAttribute;

import com.github.jklasd.test.InvokeUtil;
import com.github.jklasd.test.LazyBeanProcess;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.LazyBeanProcess.LazyBeanInitProcess;
import com.github.jklasd.test.db.LazyMongoBean;
import com.github.jklasd.test.db.TranstionalManager;
import com.github.jklasd.test.mq.LazyMQBean;
import com.github.jklasd.test.spring.LazyConfigurationPropertiesBindingPostProcessor;
import com.github.jklasd.test.spring.xml.XmlBeanUtil;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

 
@Slf4j
@SuppressWarnings("rawtypes")
public class LazyCglib implements MethodInterceptor {
	@Getter
	private Class tag;
	private String beanName;
	private boolean hasParamConstructor;
	private Constructor constructor;

	public LazyCglib( Class tag) {
		this(tag,false);
	}
	public LazyCglib(Class tag,String beanName) {
		this(tag, beanName, false);
	}
	public LazyCglib(Class tag,String beanName,Boolean hasParamConstructor) {
		this.tag = tag;
		this.beanName = beanName;
		setConstuctor(hasParamConstructor);
	}
	public LazyCglib(Class tag,Boolean hasParamConstructor) {
		this.tag = tag;
		setConstuctor(hasParamConstructor);
	}
	@Getter
	private Map<String, Object> attr = Maps.newHashMap();
	private boolean inited;
	private Map<String,String> beanMethods = Maps.newHashMap();
	@Getter
	private LazyBeanInitProcess initedProcess = new LazyBeanInitProcess() {
		public void init(Map<String, Object> attrParam) {
		    inited = true;
			if(attrParam != null) {
				attr.putAll(attrParam);
			}
			if(tagertObj != null) {
				initAttr();
			}
		}

		@Override
		public void initMethod(Map<String,String> methods) {
			beanMethods.putAll(methods);
		}
	};

	public LazyCglib(Class beanClass, String beanName2, boolean hasParamConstructor) {
		this.tag = beanClass;
		this.beanName = beanName2;
		setConstuctor(hasParamConstructor);
	}
	
	
	private Set<Class> noPackage = Sets.newHashSet();
	private void setConstuctor(Boolean hasParamConstructor) {
		if(noPackage.isEmpty()) {
			noPackage.add(int.class);
			noPackage.add(double.class);
			noPackage.add(long.class);
			noPackage.add(float.class);
			noPackage.add(byte.class);
			noPackage.add(char.class);
			noPackage.add(boolean.class);
			noPackage.add(short.class);
		}
		if(hasParamConstructor) {
			this.hasParamConstructor = hasParamConstructor;
			Constructor[] cs = tag.getConstructors();
			int count = 10;
			for(Constructor c : cs) {
				Class tagC = c.getDeclaringClass();
				if(c.getParameterCount()<count) {
					this.constructor = c;
					count = c.getParameterCount();
				}
			}
			if(cs.length <1) {
				cs = tag.getDeclaredConstructors();
				for(Constructor c : cs) {
					Class tagC = c.getDeclaringClass();
					if(c.getParameterCount()<count) {
						this.constructor = c;
						constructor.setAccessible(true);
						count = c.getParameterCount();
					}
				}
			}
		}
		
		Method[] ms = tag.getDeclaredMethods();
		for(Method m : ms) {
			if(Modifier.isFinal(m.getModifiers())
					&& Modifier.isPublic(m.getModifiers())) {
				this.hasFinal = true;
				break;
			}
		}
	}
	@Getter
	private boolean hasFinal;
	@Override
	public Object intercept(Object poxy, Method method, Object[] param, MethodProxy arg3) throws Throwable {
		try {
			Object oldObj = null;
			try {
				oldObj = AopContext.currentProxy();
			} catch (IllegalStateException e) {
			}
			
			Object newObj = getTagertObj();
			if(inited && tagertObj != null) {
                initAttr();
            }
			
			if(!Modifier.isPublic(method.getModifiers())) {
//			    log.warn("非公共方法 class:{},method:{}",tag,method.getName());
			    if(!method.isAccessible()) {
			        method.setAccessible(true);
			    }
			    if(newObj!=null) {
			        return method.invoke(newObj, param);
			    }
			    return null;
			}
			AopContextSuppert.setProxyObj(poxy);
			
			LazyBeanProcess.processLazyConfig(newObj, method,param);
			TranstionalManager.getInstance().processAnnoInfo(method, newObj);
			TransactionAttribute oldTxInfo = TranstionalManager.getInstance().getTxInfo();
            TransactionAttribute txInfo = TranstionalManager.getInstance().processAnnoInfo(method, newObj);
            TransactionStatus txStatus = null;
            if(txInfo != null) {
                TranstionalManager.getInstance().setTxInfo(txInfo);
                if(oldTxInfo!=null) {
                    //看情况开启新事务
                    if(txInfo.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED
                        || txInfo.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
                        txStatus = TranstionalManager.getInstance().beginTx(txInfo);
                    }
                }else {
                    //开启事务
                    txStatus = TranstionalManager.getInstance().beginTx(txInfo);
                }
            }
			Object result = method.invoke(newObj, param);
			if(txStatus != null) {
			    TranstionalManager.getInstance().commitTx(txStatus);
			    TranstionalManager.getInstance().clearThradLocal();
			}
			if(oldTxInfo != null) {
                TranstionalManager.getInstance().setTxInfo(oldTxInfo);
            }
			TranstionalManager.getInstance().processAnnoInfo(method, newObj);
			AopContextSuppert.setProxyObj(oldObj);
			return result;
		} catch (Exception e) {
			log.error("LazyCglib#intercept ERROR=>{}#{}==>Message:{}",tag.getName(),method.getName(),e.getMessage());
			Throwable tmp = e;
			if(e.getCause()!=null) {
				tmp = e.getCause();
			}
			throw tmp;
		}
	}

	public Object[] getArguments() {
		Object[] objes = new Object[constructor.getParameters().length];
		for(int i=0;i<objes.length;i++) {
			Class c = getArgumentTypes()[i];
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
	public Class[] getArgumentTypes() {
		return constructor.getParameterTypes();
	}
	private Object tagertObj;
	/**
	 * CGLIB
	 * 当调用目标对象方法时，对目标对象tagertObj进行实例化
	 * @return 目标对象
	 */
	@SuppressWarnings("unchecked")
	protected Object getTagertObj() {
//		if(tag.getName().contains("DruidDataSource")) {
//			log.info("断点");
//		}
		if(tagertObj != null) {
			if(tagertObj.getClass().getSimpleName().contains("com.sun.proxy")) {
				log.warn("循环处理代理Bean问题=>{}",tag);
				if(tagertObj.getClass().getSimpleName().contains(tag.getSimpleName())) {
					tagertObj = null;
				}
			}else {
				return tagertObj;
			}
		}
		if(!ScanUtil.exists(tag)) {
			if(LazyMongoBean.isMongo(tag)) {//，判断是否是Mongo
				tagertObj = LazyMongoBean.buildBean(tag,beanName);
			}else if(LazyMQBean.isBean(tag)) {
				tagertObj = LazyMQBean.buildBean(tag);
			}
			if(tagertObj==null && !inited) {
				tagertObj = LazyBean.findCreateBeanFromFactory(tag,beanName);
			}
		}
		if (tagertObj == null) {
			ConfigurationProperties propConfig = (ConfigurationProperties) tag.getAnnotation(ConfigurationProperties.class);
			if(tagertObj == null){
				if(!LazyBean.existBean(tag)) {
					if(!XmlBeanUtil.getInstance().containClass(tag)) {
						if(propConfig==null 	|| !ScanUtil.findCreateBeanForConfigurationProperties(tag)) {
							throw new RuntimeException(tag.getName()+" Bean 不存在");
						}
					}
				}
				
				if(hasParamConstructor) {
					try {
						tagertObj = constructor.newInstance(getArguments());
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						log.error("带参构造对象异常",e);
					}
				}else {
					/**
					 * 待优化
					 */
					try {//直接反射构建目标对象
						tagertObj = tag.newInstance();
						LazyBean.processAttr(tagertObj, tag);//递归注入代理对象
					} catch (InstantiationException | IllegalAccessException e) {
						log.error("构建bean=>{}",tag);
						log.error("构建bean异常",e);
					}
				}
			}
			if(propConfig!=null && tagertObj!=null) {
				LazyConfigurationPropertiesBindingPostProcessor.processConfigurationProperties(tagertObj,propConfig);
			}
		}
		return tagertObj;
	}
	private void initAttr() {
		inited = false;
		attr.forEach((k,v)->{
			Object value = v;
			if(v!=null) {
			    if(v.toString().contains("ref:")) {
			        value = LazyBean.buildProxy(null, v.toString().replace("ref:", ""));
			    }
			    LazyBean.setAttr(k, tagertObj, tag, value);
			}
		});
		beanMethods.keySet().stream().filter(key -> Objects.equal(key, "init-method")).forEach(key -> {
			InvokeUtil.invokeMethod(tagertObj, beanMethods.get(key));
		});
	}
	public boolean isNoConstructor() {
		return hasParamConstructor;
	}
	public void setNoConstructor(boolean noConstructor) {
		this.hasParamConstructor = noConstructor;
	}

}