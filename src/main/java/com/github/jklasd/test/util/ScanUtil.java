package com.github.jklasd.test.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.github.jklasd.test.core.facade.loader.PropResourceLoader;
import com.github.jklasd.test.core.facade.scan.BeanCreaterScan;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.model.AssemblyDTO;
import com.github.jklasd.test.lazyplugn.spring.JavaBeanUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author jubin.zhang
 *
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class ScanUtil {
	public static final String SPRING_PACKAGE = "org.springframework";
	public static final String BOOT_AUTO_CONFIG = "org.springframework.boot.autoconfigure";
	private static String CLASS_SUFFIX = ".class";
	static Map<String,Class<?>> nameMap = Maps.newConcurrentMap();
	private static PathMatchingResourcePatternResolver resourceResolver;
	
	/**
	 * 扫描路径下资源
	 * @param path classpath:下的文件路径
	 * @return 返回存在的资源路径数组
	 * @throws IOException 读取文件异常
	 */
	public static Resource[] getResources(String path) throws IOException {
		if(resourceResolver == null) {
			resourceResolver = new PathMatchingResourcePatternResolver(); 
		}
		return resourceResolver.getResources(path);
	}
	public static boolean exists(Class record) {
		return nameMap.values().contains(record);
	}
	
	public static Map<String, Class<?>> findClassMap(String scanPath) {
		return scaner.findClassMap(scanPath);
	}
	
	private static boolean init = false;
	/**
	 * 加载所有class，缓存起来
	 * 类似加载 AbstractEmbeddedServletContainerFactory
	 */
	private static ClassScan scaner= ClassScan.getInstance();
	public static void loadAllClass() {
		if(init) {
			return;
		}
		init = true;
		scaner.scan();
	}
//	private static Set<String> autoConfigClass = Sets.newConcurrentHashSet();
//	private static Map<String,Class<?>> autoConfigMap = Maps.newConcurrentMap();
	public static void loadContextPathClass() {
		scaner.loadContextPathClass();
	}
	
	public static Class findClassByName(String beanName) {
		return scaner.findClassByName(beanName);
	}
	
	
	public static Boolean isInScanPath(Class<?> requiredType) {
		return nameMap.containsKey(requiredType.getName());
	}
	/**
	 * 扫描继承abstractClass 的类
	 * @param abstractClass 接口
	 * @return 返回继承abstractClass 的类
	 */
	public static List<Class<?>> findClassExtendAbstract(Class abstractClass){
		return findClassExtendAbstract(abstractClass, null,null);
	}
	public static List<Class<?>> findClassExtendAbstract(Class abstractClass,Map<String,Class> classMap,String ClassName){
		Map<String,Class> tmp = Maps.newHashMap();
		if(classMap!=null) {
			tmp.putAll(classMap);
		}
		tmp.putAll(nameMap);
		List<Class<?>> list = Lists.newArrayList();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(tmp.keySet()))
		.runAndWait(name ->{
			if(ClassName!=null && name.equals(ClassName)) {
				return;
			}
			Class<?> tmpClass = tmp.get(name);
			if(isExtends(tmpClass,abstractClass)) {
				if((tmpClass.getAnnotation(Component.class)!=null || tmpClass.getAnnotation(Service.class)!=null)
						&& !Modifier.isAbstract(tmpClass.getModifiers())) {
					list.add(tmpClass);
				}
			}
		});
		return list;
	}
	public static Class findClassImplInterfaceByBeanName(Class interfaceClass,Map<String,Class> classMap,String beanName){
	    if(StringUtils.isBlank(beanName)) {
	        throw new JunitException();
	    }
        Map<String,Class> tmp = Maps.newHashMap();
        if(classMap!=null) {
            tmp.putAll(classMap);
        }
        tmp.putAll(nameMap);
        List<Class> list = Lists.newArrayList();
        JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(tmp.keySet()))
        .runAndWait(name ->{
            Class<?> tmpClass = tmp.get(name);
            if(isImple(tmpClass,interfaceClass)) {
                if(Objects.equals(beanName, LazyBean.getBeanName(tmpClass))) {
                    list.add(tmpClass);
                }
            }
        });
        if(list.isEmpty()) {
            log.warn("没有找到相关实现类========{}======={}==",interfaceClass,beanName);
        }
        return list.isEmpty() ? null : list.get(0);
    }
	public static List<Class<?>> findClassImplInterface(Class interfaceClass,Map<String,Class<?>> classMap,String ClassName){
		Map<String,Class> tmp = Maps.newHashMap();
		if(classMap!=null) {
			tmp.putAll(classMap);
		}
		tmp.putAll(nameMap);
		List<Class<?>> list = Lists.newArrayList();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(tmp.keySet()))
		.runAndWait(name ->{
			if(ClassName!=null && name.equals(ClassName)) {
				return;
			}
			Class<?> tmpClass = tmp.get(name);
			if(isImple(tmpClass,interfaceClass)) {
				if((tmpClass.getAnnotation(Component.class)!=null || tmpClass.getAnnotation(Service.class)!=null)
						&& !Modifier.isAbstract(tmpClass.getModifiers())) {
					list.add(tmpClass);
				}
			}
		});
		return list;
	}
	/**
	 * 扫描实现了interfaceClass 的类
	 * @param interfaceClass 接口
	 * @return 返回实现 interfaceClass 的类
	 */
	public static List<Class<?>> findClassImplInterface(Class interfaceClass){
		return scaner.findClassImplInterface(interfaceClass);
	}
	/**
	 * 判断 c 是否是interfaceC的实现类
	 * @param implClass 实现类型
	 * @param interfaceClass 接口类型
	 * @return  true/ false
	 */
	public static boolean isImple(Class implClass,Class<?> interfaceClass) {
		return !implClass.isInterface() && interfaceClass.isAssignableFrom(implClass);
	}
	/**
	 * 判断 subClass 是否继承 abstractClass
	 * @param subClass 子类
	 * @param abstractClass 父类
	 * @return true/false
	 */
	public static boolean isExtends(Class subClass,Class<?> abstractClass) {
		return abstractClass.isAssignableFrom(subClass);
	}
	
	/**
	 * 扫描类 for class
	 * @param annotationType 注解类型
	 * @return 存在 annotationType 注解的类
	 */
//	public static List<Class<?>> findClassWithAnnotation(Class<? extends Annotation> annotationType){
//		return findClassWithAnnotation(annotationType, nameMap);
//	}
	
	public static List<Class<?>> findClassWithAnnotation(Class<? extends Annotation> annotationType,Map<String,Class<?>> nameMapTmp){
		List<Class<?>> list = Lists.newArrayList();
		JunitCountDownLatchUtils.buildCountDownLatch(nameMapTmp.keySet().stream().filter(name->!notFoundSet.contains(name)).collect(Collectors.toList()))
		.setException((name,e)->notFoundSet.add(name))
		.runAndWait(name ->{
			Class<?> c = nameMapTmp.get(name);
//			try {
				Annotation type = c.getDeclaredAnnotation(annotationType);
				if(type != null) {
					list.add(c);
				}
//			} catch (Exception e) {
//				log.error("#findClassWithAnnotation ERROR",e);
//			}
		});
		return list;
	}
	public static List<Class<?>> findStaticMethodClass() {
		Set<Class<?>> list = Sets.newHashSet();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(nameMap.keySet()))
		.setException((name,e)->{
		    log.error("遗漏#findStaticMethodClass#=>{}",name);
		})
		.runAndWait(name ->{
			Class<?> c = nameMap.get(name);
			Annotation configuration = c.getDeclaredAnnotation(Configuration.class);
			boolean finding = false;
			if(configuration!=null) {
			    finding = true;
			}else {
			    Annotation service = c.getDeclaredAnnotation(Service.class);
			    Annotation comp = c.getDeclaredAnnotation(Component.class);
			    finding = service!=null || comp!=null;
			}
			if(finding) {
				Method[] methods = c.getDeclaredMethods();
				for(Method m : methods) {
					if(Modifier.isStatic(m.getModifiers())
							&& !m.getName().contains("lambda$")//非匿名方法
						&& !m.getName().contains("access$")) {//非匿名方法
						Class<?> returnType = m.getReturnType();
						if(!returnType.getName().contains("void")) {
							log.debug("method=>{}",m);
							list.add(c);
							return;
						}
//						log.debug(returnType.getName());
					}
				}
			}else if(configuration == null) {
				
			}
		});
		return Lists.newArrayList(list);
	}
	private static Set<String> notFoundSet = Sets.newConcurrentHashSet();
	private static BeanCreaterScan beanFactoryScaner = BeanCreaterScan.getInstance();
	public synchronized static Object[] findCreateBeanFactoryClass(final AssemblyDTO assemblyData) {
		return beanFactoryScaner.findCreateBeanFactoryClass(assemblyData);
	}
	public static Resource getRecource(String location) throws IOException {
		Resource[] rs = getResources(location);
		return rs.length>0?rs[0]:null;
	}
	public static Class getClassByName(String className) {
		return nameMap.get(className);
	}
	
	public static Resource getRecourceAnyOne(String... paths) throws IOException {
		for(String path: paths) {
			Resource r = getRecource(path);
			if(r!=null && r.exists()) {
				return r;
			}
		}
		return null;
	}
	private static PropResourceLoader propLoader = PropResourceLoader.getInstance();
	public static boolean findCreateBeanForConfigurationProperties(Class tag) {
		return propLoader.contains(tag);
	}
	private static Set<String> notFundClassSet = Sets.newConcurrentHashSet();
	
	public static Class loadClass(String className) {
		if(notFundClassSet.contains(className))
			return null;
		try {
			Class classObj = Class.forName(className, false, JavaBeanUtil.class.getClassLoader());
			return classObj;
		} catch (NoClassDefFoundError e) {
			log.warn("#NoClassDefFoundError=>{}",className);
			notFundClassSet.add(className);
		} catch (ClassNotFoundException e) {
			log.warn("#ClassNotFoundException=>{}",className);
			notFundClassSet.add(className);
		}
		return null;
	}
	
	public static  boolean isBasicClass(Class cal){
		return cal == Integer.class || cal == int.class
				|| cal == Boolean.class || cal == boolean.class
				|| cal == Short.class || cal == short.class
				|| cal == Double.class || cal == double.class
				|| cal == Long.class || cal == long.class
				|| cal == Float.class || cal == float.class;
	}
    public static Type[] getGenericType(Class<?> tagClass) {
        for(Type t:tagClass.getGenericInterfaces()) {
            if(t instanceof ParameterizedType) {
                ParameterizedType  pType = (ParameterizedType) t;
                return pType.getActualTypeArguments();
            }
        }
        return null;
    }
}