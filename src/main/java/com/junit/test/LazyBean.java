package com.junit.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContextSuppert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.junit.test.dubbo.LazyDubboBean;
import com.junit.test.mapper.LazyMybatisMapperBean;
import com.junit.test.mq.LazyMQBean;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang
 * 2020-11-19
 */
@Slf4j
public class LazyBean {

	public static Map<Class, Object> singleton = Maps.newHashMap();
	public static Map<String, Object> singletonName = Maps.newHashMap();
	/**
	 * 构建代理对象
	 * @param classBean
	 * @return
	 */
	public static Object buildProxy(Class classBean,String beanName) {
		if(singletonName.containsKey(beanName)) {
			return singletonName.get(beanName);
		}
		if(StringUtils.isBlank(beanName)) {
			if (singleton.containsKey(classBean)) {
				return singleton.get(classBean);
			}
		}
		Object tag = null;
		if(classBean.getPackage().getName().contains(LazyMQBean.packageName)) {
			try {
				tag = LazyMQBean.buildBean(classBean);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}else {
			try {
				if (classBean.isInterface()) {
					InvocationHandler handler = new LazyImple(classBean,beanName);
					tag = Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[] { classBean }, handler);
					
				} else {
					MethodInterceptor handler = new LazyCglib(classBean,beanName);
					tag = Enhancer.create(classBean, handler);
				}
			} catch (Exception e) {
				tag = TestUtil.getExistBean(classBean, beanName);
				if(tag == null) {
					System.out.println("[ERROR]代理Bean=>"+classBean+"=>"+beanName);
				}
			}
		}
		if(tag!=null) {
			if(StringUtils.isNotBlank(beanName)) {
				singletonName.put(beanName, tag);
			}else {
				singleton.put(classBean, tag);
			}
		}
		return tag;
	}
	/**
	 * 构建代理对象
	 * @param classBean
	 * @return
	 */
	public static Object buildProxy(Class classBean) {
		if (singleton.containsKey(classBean)) {
			return singleton.get(classBean);
		}
		Object tag = buildProxy(classBean,null);
		if(tag!=null) {
			singleton.put(classBean, tag);
		}
		return tag;
	}
	public static void setObj(Field f,Object obj,Object proxyObj) {
		setObj(f, obj, proxyObj, null);
	}
	public static void setObj(Field f,Object obj,Object proxyObj,String proxyBeanName) {
		if(proxyObj == null) {//延迟注入,可能启动时，未加载到bean
			TestUtil.loadLazyAttr(obj, f, proxyBeanName);
		}
		try {
			if (!f.isAccessible()) {
				f.setAccessible(true);
			}
			f.set(obj, proxyObj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 注入
	 * @param obj
	 * @param objClassOrSuper
	 */
	static Set<String> exist = Sets.newHashSet();
	public static void processAttr(Object obj, Class objClassOrSuper) {
//		if(objClassOrSuper.getName().contains("MultiServiceConfiguration")) {
//			log.info("需要注入=>{}=>{}",objClassOrSuper.getName());
//		}
		if(!TestUtil.isTest()) {
			TestUtil.openTest();
		}
		
		if(exist.contains(obj.hashCode()+"="+objClassOrSuper.getName())) {
			return;
		}
		exist.add(obj.hashCode()+"="+objClassOrSuper.getName());
		Field[] fields = objClassOrSuper.getDeclaredFields();
		for(Field f : fields){
			Autowired aw = f.getAnnotation(Autowired.class);
			if (aw != null) {
				if(f.getType() == List.class) {
					ParameterizedType t = (ParameterizedType) f.getGenericType();
					Type[] item = t.getActualTypeArguments();
					if(item.length == 1) {
						//处理一个集合注入
						try {
							Class<?> c = Class.forName(item[0].getTypeName());
							setObj(f, obj, ScanUtil.findListBean(c));
							System.out.println("注入成功");
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}else {
						System.out.println("其他特殊情况");
					}
				}else {
					if(TestUtil.getExistBean(f.getType(), f.getName()) != null) {
						setObj(f, obj, TestUtil.getExistBean(f.getType(), f.getName()));
					}else {
						setObj(f, obj, buildProxy(f.getType()));
					}
				}
//				String className = f.getType().getName();
//				if (className.contains("Mapper")) {
//					try {
//						if (!f.isAccessible()) {
//							f.setAccessible(true);
//						}
//						Object value = f.get(obj);
//						if (value == null) {
//							setObj(f, obj, buildProxy(f.getType()));
//						}
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				} else if (className.contains("Impl") || className.contains("Service")) {
//					setObj(f, obj, buildProxy(f.getType()));
//				} else if (className.contains("jedis")) {
//					setObj(f, obj, TestUtil.getExistBean(f.getType(), f.getName()));
//				} else {
//					
//				}
			} else {
				Value v = f.getAnnotation(Value.class);
				if (v != null) {
					setObj(f, obj, TestUtil.value(v.value().replace("${", "").replace("}", ""), f.getType()));
				} else {
					javax.annotation.Resource c = f.getAnnotation(javax.annotation.Resource.class);
					if (c != null) {
						if(StringUtils.isNotBlank(c.name())) {
							setObj(f, obj, buildProxy(f.getType(),c.name()),c.name());
						}else {
							setObj(f, obj, buildProxy(f.getType()));
						}
					} else {
						log.info("不需要需要注入=>{}", f.getName());
					}
				}
			}
		}
		Class<?> superC = objClassOrSuper.getSuperclass();
		if (superC != null) {
			processAttr(obj, superC);
		}

		Method[] ms = obj.getClass().getDeclaredMethods();
		postConstruct(obj, ms,superC);
	}

	public static void processStatic(Class c) {
		Object obj = buildProxy(c);
		processAttr(obj, c);
	}
	private static void postConstruct(Object obj, Method[] ms,Class sup) {
		if(sup != null) {
			ms = sup.getDeclaredMethods();
			postConstruct(obj, ms, sup.getSuperclass());
		}
		for (Method m : ms) {
			if (m.getAnnotation(PostConstruct.class) != null) {
				try {
					m.invoke(obj, null);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class LazyCglib implements MethodInterceptor {
	private Class tag;
	private String beanName;

	public LazyCglib(Class tag) {
		this.tag = tag;
	}
	public LazyCglib(Class tag,String beanName) {
		this.tag = tag;
		this.beanName = beanName;
	}

	@Override
	public Object intercept(Object arg0, Method arg1, Object[] arg2, MethodProxy arg3) throws Throwable {
		try {
			AopContextSuppert.setProxyObj(arg0);
			return arg1.invoke(getTagertObj(), arg2);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private Object tagertObj;

	private Object getTagertObj() {
		if (tagertObj == null) {
			if(beanName!=null) {
				tagertObj = ScanUtil.findBean(beanName);
				LazyBean.processAttr(tagertObj, tagertObj.getClass());
			}else {
				try {
					tagertObj = tag.newInstance();
					LazyBean.processAttr(tagertObj, tag);
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return tagertObj;
	}

}

@Slf4j
class LazyImple implements InvocationHandler {

	private Class tag;
	private Object tagertObj;
	private String beanName;

	public LazyImple(Class tag) {
		this.tag = tag;
	}
	public LazyImple(Class tag,String beanName) {
		this.tag = tag;
		this.beanName = beanName;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			AopContextSuppert.setProxyObj(proxy);
			return method.invoke(getTagertObj(), args);
		} catch (Exception e) {
			e.printStackTrace();
			//throw e;
		}
		return null;
	}

	private Object getTagertObj() {
		if (tagertObj == null) {
			if(LazyDubboBean.isDubbo(tag)) {
				tagertObj = LazyDubboBean.buildBean(tag);
			} else {
				if(beanName == null) {
					if(tag.getPackage().getName().contains(TestUtil.mapperScanPath)) {
						//延迟处理
						tagertObj = LazyMybatisMapperBean.buildBean(tag);
					}else {
						// 本地bean
						Object tagImp = ScanUtil.findBeanByInterface(tag);
						if(tagImp == null) {
							log.info("未找到本地Bean=>{}",tag);
						}else {
							/**
							 * 实现类是本地Bean
							 */
							tagertObj = tagImp;
							LazyBean.processAttr(tagImp, tagImp.getClass());
						}
					}
				}else {
					if(tag.getPackage().getName().contains(TestUtil.mapperScanPath)) {
						//延迟处理
						tagertObj = LazyMybatisMapperBean.buildBean(tag);
					}else {
						// 本地bean
						Object tagImp = ScanUtil.findBean(beanName);
						if(tagImp == null) {
							log.info("未找到本地Bean=>{}",tag);
						}else {
							/**
							 * 实现类是本地Bean
							 */
							tagertObj = tagImp;
							LazyBean.processAttr(tagImp, tagImp.getClass());
						}
					}
				}
				
			}
		}
		return tagertObj;
	}
}
