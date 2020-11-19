package com.junit.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.google.common.collect.Maps;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyBean {

	private static String welab;

	public static Map<Class, Object> singleton = Maps.newHashMap();
	/**
	 * 构建代理对象
	 * @param classBean
	 * @return
	 */
	public static Object buildProxy(Class classBean) {
		Object obj = buildRabbit(classBean);
		if (obj != null) {
			return obj;
		}
		if (classBean.isInterface()) {
			InvocationHandler handler = new LazyImple(classBean);
			return Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[] { classBean }, handler);

		} else {
			MethodInterceptor handler = new LazyCglib(classBean);
			return Enhancer.create(classBean, handler);
		}
	}
	/**
	 * 构建rabbit应用
	 * @param classBean
	 * @return
	 */
	private static RabbitTemplate obj = null;
	private static Object buildRabbit(Class classBean) {
		if (classBean == RabbitTemplate.class) {
			if (obj != null) {
				return obj;
			}
			try {
				obj = (RabbitTemplate) classBean.newInstance();
				// 定义一个连接工厂
				ConnectionFactory factory = new ConnectionFactory();
				// 设置服务端地址（域名地址/ip）
				factory.setHost(TestUtil.getValue("spring.rabbitmq.host"));
				// 设置服务器端口号
				factory.setPort(5672);
				// 设置虚拟主机(相当于数据库中的库)
				factory.setVirtualHost("/");
				// 设置用户名
				factory.setUsername(TestUtil.getValue("spring.rabbitmq.username"));
				// 设置密码
				factory.setPassword(TestUtil.getValue("spring.rabbitmq.password"));
				Connection connection = factory.newConnection();
				obj.setConnectionFactory(new CachingConnectionFactory(factory));

				return obj;
			} catch (InstantiationException | IllegalAccessException | IOException | TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (classBean == RabbitMessagingTemplate.class) {
			try {
				RabbitMessagingTemplate objM = (RabbitMessagingTemplate) classBean.newInstance();
				objM.setRabbitTemplate(obj == null ? (RabbitTemplate) buildProxy(RabbitTemplate.class) : obj);
				return objM;
			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * 注入
	 * @param obj
	 * @param objClassOrSuper
	 */
	public static void processAttr(Object obj, Class objClassOrSuper) {
		Field[] fields = objClassOrSuper.getDeclaredFields();
		for (Field f : fields) {
			Autowired aw = f.getAnnotation(Autowired.class);
			if (aw != null) {
//				if(f.getName().equals("productConfigService")) {
//					log.info("需要注入=>{}=>{}",f.getName(),f.getType().getName());
//				}
				String className = f.getType().getName();
				if (className.contains("Mapper")) {
					try {
						if (!f.isAccessible()) {
							f.setAccessible(true);
						}
						Object value = f.get(obj);
						if (value == null) {
							f.set(obj, TestUtil.getExistBean(f.getType(), f.getName()));
						}
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				} else if (className.contains("Impl") || className.contains("Service")) {
					try {
						if (!f.isAccessible()) {
							f.setAccessible(true);
						}
						Object proxyObj = null;
						if (!singleton.containsKey(f.getType())) {
							proxyObj = buildProxy(f.getType());
							singleton.put(f.getType(), proxyObj);
						}
						proxyObj = singleton.get(f.getType());
						f.set(obj, proxyObj);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (className.contains("jedis")) {
					try {
						if (!f.isAccessible()) {
							f.setAccessible(true);
						}
						f.set(obj, TestUtil.getExistBean(f.getType(), f.getName()));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				} else {
					log.info(className);
					try {
						if (!f.isAccessible()) {
							f.setAccessible(true);
						}
						Object proxyObj = null;
						if (!singleton.containsKey(f.getType())) {
							proxyObj = buildProxy(f.getType());
							singleton.put(f.getType(), proxyObj);
						}
						proxyObj = singleton.get(f.getType());
						f.set(obj, proxyObj);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				Value v = f.getAnnotation(Value.class);
				if (v != null) {
					try {
						if (!f.isAccessible()) {
							f.setAccessible(true);
						}
						f.set(obj, TestUtil.value(v.value().replace("${", "").replace("}", ""), f.getType()));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				} else {
					Component c = f.getAnnotation(Component.class);
					if (c != null) {

					} else {
						log.info("不需要需要注入=>{}", f.getName());
					}
				}
			}
		}
		Class superC = objClassOrSuper.getSuperclass();
		if (superC != null) {
			processAttr(obj, superC);
		}

		Method[] ms = obj.getClass().getDeclaredMethods();
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

	public static String getWelab() {
		if(welab == null) {
			welab = "com."+TestUtil.getValue("app.id").replace("-", ".");
		}
		return welab;
	}
}

class LazyCglib implements MethodInterceptor {
	private Class tag;

	public LazyCglib(Class tag) {
		this.tag = tag;
	}

	@Override
	public Object intercept(Object arg0, Method arg1, Object[] arg2, MethodProxy arg3) throws Throwable {
		try {
			return arg1.invoke(getTagertObj(), arg2);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private Object tagertObj;

	private Object getTagertObj() {
		if (tagertObj == null) {
			try {
				tagertObj = tag.newInstance();
				LazyBean.processAttr(tagertObj, tag);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return tagertObj;
	}

}

@Slf4j
class LazyImple implements InvocationHandler {

	private Class tag;
	private Object tagertObj;

	public LazyImple(Class tag) {
		this.tag = tag;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			return method.invoke(getTagertObj(), args);
		} catch (Exception e) {
			e.printStackTrace();
			//throw e;
		}
		return null;
	}

	private Object getTagertObj() {
		if (tagertObj == null) {
			if (!tag.getName().contains(LazyBean.getWelab())) {
				// 设定为dubbo
				tagertObj = buildDubboService(tag);
			} else {
				if(tag.getName().contains("Mapper")) {
					//延迟处理
					tagertObj = TestUtil.getExistBean(tag);
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
			}
		}
		return tagertObj;
	}
	
	private static RegistryConfig registryConfig;
	
	private static Map<Class<?>,Object> dubboSingle = Maps.newHashMap();
	public static Object buildDubboService(Class<?> dubboClass) {
		if(dubboSingle.containsKey(dubboClass)) {
			return dubboSingle.get(dubboClass);
		}
		ReferenceConfig<?> referenceConfig = new ReferenceConfig<>();
		referenceConfig.setInterface(dubboClass);
		String groupStr = dubboClass.getName().replace("com.welab.", "");
		String[] keys = groupStr.split("\\.");
		for (String k : keys) {
			String v = TestUtil.getValue("dubbo.group." + k);
			if (v != null) {
				groupStr = v;
				log.info("{}=>{}", dubboClass.getName(), groupStr);
				break;
			}
		}
		referenceConfig.setGroup(groupStr);
		ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-examples-consumer");
		if(registryConfig == null) {
			registryConfig = new RegistryConfig("zookeeper://" + TestUtil.getValue("zookeeper.url"));
			registryConfig.setUsername(TestUtil.getValue("zookeeper.username"));
			registryConfig.setPassword(TestUtil.getValue("zookeeper.password"));
			registryConfig.setClient("curator");
			registryConfig.setSubscribe(true);
			registryConfig.setRegister(false);
		}
		referenceConfig.setApplication(applicationConfig);
		referenceConfig.setRegistry(registryConfig);
		Object obj = referenceConfig.get();
		dubboSingle.put(dubboClass,obj);
		return obj;
	}

}
@Slf4j
class ScanUtil{
	public static Object findBean(String beanName) {
		try {
			Resource[] resources = new PathMatchingResourcePatternResolver()
					.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" + LazyBean.getWelab().replace(".", "/"));
			for (Resource r : resources) {
				Class tag = findClass(r.getFile(), beanName);
				if (tag != null) {
					if(tag.isInterface() && !tag.getName().contains(LazyBean.getWelab())) {
						return LazyImple.buildDubboService(tag);
					}
					return LazyBean.buildProxy(tag);
				}
			}
		} catch (IOException | ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	public static Object findBean(Class<?> requiredType) {
		try {
			if(requiredType.isInterface() && !requiredType.getName().contains(LazyBean.getWelab())) {
				return LazyImple.buildDubboService(requiredType);
			}
			Resource[] resources = new PathMatchingResourcePatternResolver()
					.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" +LazyBean.getWelab().replace(".", "/"));
			for (Resource r : resources) {
				if(requiredType.isInterface()) {
					Class tag = findClassByInterface(r.getFile(), requiredType);
					if (tag != null) {
						return LazyBean.buildProxy(tag);
					}
				}else {
					Class tag = findClass(r.getFile(), requiredType);
					if (tag != null) {
						return LazyBean.buildProxy(tag);
					}
				}
			}
		} catch (IOException | ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	private static Class findClass(File file, Class<?> requiredType) {
		File[] files = file.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				// 递归
				Class tag = findClass(f, requiredType);
				if (tag != null) {
					return tag;
				}
			} else if (f.getName().endsWith(".class")) {
				String p = f.getPath();
				p = p.substring(p.indexOf(LazyBean.getWelab().replace(".", "\\"))).replace("\\", ".").replace(".class", "");
				// 查看是否class
				try {
					Class<?> c = Class.forName(p);
					if(c == requiredType) {
						return c;
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}else {
				log.info("=============其他文件=============");
			}
		}
		return null;
	}
	/**
	 * 扫描类 for bean
	 * @param file
	 * @param beanName
	 * @return
	 * @throws ClassNotFoundException
	 */
	private static Class findClass(File file, String beanName) throws ClassNotFoundException {
		File[] files = file.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				// 递归
				Class tag = findClass(f, beanName);
				if (tag != null) {
					return tag;
				}
			} else if (f.getName().endsWith(".class")) {
				String p = f.getPath();
				p = p.substring(p.indexOf(LazyBean.getWelab().replace(".", "\\"))).replace("\\", ".").replace(".class", "");
				// 查看是否class
//				log.info(f.getPath());
				if (beanName.toLowerCase().equals(f.getName().replace(".class", ""))) {
					return Class.forName(p);
				} else {
					Class<?> c = Class.forName(p);
					Service ann = (Service) c.getAnnotation(Service.class);
					if (ann != null) {
						if (Objects.equals(ann.value(), beanName)) {
							return c;
						}
					}
				}
			} else {
				log.info("=============其他文件=============");
			}
		}
		return null;
	}
	public static Object findBeanByInterface(Class interfaceClass) {
		try {
			Resource[] resources = new PathMatchingResourcePatternResolver()
					.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" + LazyBean.getWelab().replace(".", "/"));
			for (Resource r : resources) {
				Class tag = findClassByInterface(r.getFile(), interfaceClass);
				if (tag != null) {
					return LazyBean.buildProxy(tag);
				}
			}
		} catch (IOException | ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	/**
	 * 扫描类 for class
	 * @param file
	 * @param interfaceClass
	 * @return
	 * @throws ClassNotFoundException
	 */
	private static Class findClassByInterface(File file, Class interfaceClass) throws ClassNotFoundException {
		File[] files = file.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				// 递归
				Class tag = findClassByInterface(f, interfaceClass);
				if (tag != null) {
					return tag;
				}
			} else if (f.getName().endsWith(".class")) {
				String p = f.getPath();
				p = p.substring(p.indexOf(LazyBean.getWelab().replace(".", "\\"))).replace("\\", ".").replace(".class", "");
				// 查看是否class
				Class<?> c = Class.forName(p);
				Class[] fcs  = c.getInterfaces();
				for(int i=0;i<fcs.length;i++) {
					if(Objects.equals(fcs[i], interfaceClass)) {
						return c;
					}
				}
			} else {
			}
		}
		return null;
	}
}