package com.github.jklasd.test.common.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.interf.register.BeanScanI;
import com.github.jklasd.test.common.interf.register.PropResourceManagerI;
import com.github.jklasd.test.common.interf.register.Scan;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.model.JunitMethodDefinition;
import com.google.common.collect.Lists;
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
		return scaner.isInScanPath(record);
	}
	@Deprecated
	public static Map<String, Class<?>> findClassMap(String scanPath) {
		return getScanner().findClassMap(scanPath);
	}
	
	private static volatile boolean init = false;
	/**
	 * 加载所有class，缓存起来
	 * 类似加载 AbstractEmbeddedServletContainerFactory
	 */
	private static Scan scaner;
	
	public static Scan getScanner() {
		if(scaner == null) {
			scaner= ContainerManager.getComponent(Scan.class.getSimpleName());
		}
		return scaner;
	}
	
	public static void loadAllClass() {
		if(isInit()) {
			return;
		}
		getScanner().scan();
		init = true;
	}
	
//	@Deprecated
//	public static void loadContextPathClass() {
//		getScanner().loadContextPathClass();
//	}
	
	public static Class findClassByName(String beanName) {
		return getScanner().findClassByName(beanName);
	}
	
	
	public static Boolean isInScanPath(Class<?> requiredType) {
		return getScanner().isInScanPath(requiredType);
	}
	/**
	 * 扫描继承abstractClass 的类
	 * @param abstractClass 接口
	 * @return 返回继承abstractClass 的类
	 */
	public static List<Class<?>> findClassExtendAbstract(Class abstractClass){
		return getScanner().findClassExtendAbstract(abstractClass);
	}
	

	public static List<Class<?>> findSubClass(Class<?> requiredType) {
		List<Class<?>> tags = null;
		if(requiredType.isInterface()) {
			tags = getScanner().findClassImplInterface(requiredType);
		}else {
			tags = getScanner().findClassExtendAbstract(requiredType);
		}
		return tags;
	}
	
	/**
	 * 扫描实现了interfaceClass 的类
	 * @param interfaceClass 接口
	 * @return 返回实现 interfaceClass 的类
	 */
	public static List<Class<?>> findClassImplInterface(Class interfaceClass){
		return getScanner().findClassImplInterface(interfaceClass);
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
	
	public static List<Class<?>> findClassWithAnnotation(Class<? extends Annotation> annotationType,Map<String,Class<?>> nameMapTmp){
		List<Class<?>> list = Lists.newArrayList();
		JunitCountDownLatchUtils.buildCountDownLatch(nameMapTmp.keySet().stream().filter(name->!notFoundSet.contains(name)).collect(Collectors.toList()))
		.setException((name,e)->notFoundSet.add(name))
		.runAndWait(name ->{
			Class<?> c = nameMapTmp.get(name);
			Annotation type = c.getDeclaredAnnotation(annotationType);
			if(type != null) {
				list.add(c);
			}
		});
		return list;
	}
	
	private static Set<String> notFoundSet = Sets.newConcurrentHashSet();
	private static volatile BeanScanI beanFactoryScaner;

	public /* synchronized */static JunitMethodDefinition findCreateBeanFactoryClass(final BeanModel assemblyData) {
		if(beanFactoryScaner == null) {
			beanFactoryScaner = ContainerManager.getComponent(BeanScanI.class.getSimpleName());
			if(beanFactoryScaner == null) {
				log.warn("beanFactoryScaner 未加载到");
				return null;
			}
		}
		return beanFactoryScaner.findCreateBeanFactoryClass(assemblyData);
	}
	
	public static Resource getRecource(String location) throws IOException {
		Resource[] rs = getResources(location);
		return rs.length>0?rs[0]:null;
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
	private static PropResourceManagerI propLoader;
	public static boolean findCreateBeanForConfigurationProperties(Class tag) {
		if(propLoader == null) {
			propLoader = ContainerManager.getComponent(PropResourceManagerI.class.getSimpleName());
		}
		return propLoader.contains(tag);
	}
	private static Set<String> notFundClassSet = Sets.newConcurrentHashSet();
	
	public static Class loadClass(String className) {
		if(notFundClassSet.contains(className))
			return null;
		try {
			Class classObj = JunitClassLoader.getInstance().loadClass(className);
			return classObj;
		} catch (NoClassDefFoundError e) {
			log.debug("#NoClassDefFoundError=>{}",className);
			notFundClassSet.add(className);
		} catch (ClassNotFoundException e) {
			log.debug("#ClassNotFoundException=>{}",className);
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
	public static boolean isInit() {
		return init;
	}
	public static List<JunitMethodDefinition> findCreateBeanFactoryClasses(BeanModel assemblyData) {
		if(beanFactoryScaner == null) {
			beanFactoryScaner = ContainerManager.getComponent(BeanScanI.class.getSimpleName());
			if(beanFactoryScaner == null) {
				log.warn("beanFactoryScaner 未加载到");
				return null;
			}
		}
		return beanFactoryScaner.findCreateBeanFactoryClasses(assemblyData);
	}
}