package com.github.jklasd.test;

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

import com.github.jklasd.test.db.LazyMongoBean;
import com.github.jklasd.test.mq.LazyMQBean;
import com.github.jklasd.test.spring.LazyConfigurationPropertiesBindingPostProcessor;
import com.github.jklasd.test.spring.XmlBeanUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

 
@Slf4j
@SuppressWarnings("rawtypes")
public class LazyCglib implements MethodInterceptor {
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
	private Map<String, Object> attr;
	public LazyCglib(Class beanClass, String beanName, Map<String, Object> attr) {
		this(beanClass, beanName, false,attr);
	}

	public LazyCglib(Class beanClass, String beanName2, boolean hasParamConstructor, Map<String, Object> attr) {
		this.tag = beanClass;
		this.beanName = beanName2;
		this.attr = attr;
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
	public Object intercept(Object arg0, Method arg1, Object[] arg2, MethodProxy arg3) throws Throwable {
		try {
			if(!arg1.isAccessible()) {
				if(!Modifier.isPublic(arg1.getModifiers())) {
					log.warn("非公共方法，代理执行会出现异常 class:{},method:{}",tag,arg1);
					return null;
				}
			}
			Object oldObj = null;
			try {
				oldObj = AopContext.currentProxy();
			} catch (IllegalStateException e) {
			}
			Object newObj = getTagertObj();
			if(newObj != null) {
				AopContextSuppert.setProxyObj(newObj);
			}
			Object result = arg1.invoke(newObj, arg2);
			AopContextSuppert.setProxyObj(oldObj);
			return result;
		} catch (Exception e) {
			log.error("LazyCglib#intercept ERROR=>{}#{}==>Message:{}",tag.getName(),arg1.getName(),e.getMessage());
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
	 * @return
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
			if(tagertObj==null) {
				tagertObj = LazyBean.findCreateBeanFromFactory(tag,beanName);
			}
		}
		if (tagertObj == null) {
			ConfigurationProperties propConfig = (ConfigurationProperties) tag.getAnnotation(ConfigurationProperties.class);
//			if(StringUtils.isNotBlank(beanName)) {//若存在beanName。则通过beanName查找
//				tagertObj = LazyBean.findBean(beanName);
//				if(tagertObj != null) {
//					LazyBean.processAttr(tagertObj, tagertObj.getClass());//递归注入代理对象
//				}
//			}
			if(tagertObj == null){
				if(!LazyBean.existBean(tag)) {
					if(!XmlBeanUtil.containClass(tag)) {
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
		if(attr != null && tagertObj != null) {
			attr.forEach((k,v)->{
				Object value = v;
				if(v.toString().contains("ref:")) {
					value = LazyBean.buildProxy(null, v.toString().replace("ref:", ""));
				}
				LazyBean.setAttr(k, tagertObj, tag, value);
			});
		}
		return tagertObj;
	}
	public boolean isNoConstructor() {
		return hasParamConstructor;
	}
	public void setNoConstructor(boolean noConstructor) {
		this.hasParamConstructor = noConstructor;
	}

}