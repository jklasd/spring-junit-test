package com.github.jklasd.test.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.github.jklasd.test.AssemblyUtil;
import com.github.jklasd.test.InvokeUtil;
import com.github.jklasd.test.LazyBean;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.db.LazyMybatisMapperBean;
import com.github.jklasd.test.dubbo.LazyDubboBean;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("rawtypes")
public class JavaBeanUtil {
	private static Map<Class,Object> factory = Maps.newHashMap();
	private static Map<String,Object> cacheBean = Maps.newHashMap();
	public static Object buildBean(Class configClass, Method method, AssemblyUtil assemblyData) {
		String key = assemblyData.getTagClass()+"=>beanName:"+assemblyData.getBeanName();
		if(cacheBean.containsKey(key)) {
			return cacheBean.get(key);
		}
		try {
			if(!factory.containsKey(configClass)) {
//				log.info("Class=>{},  method=>{},  param=>{}",configClass.getSimpleName(),method.getName(),method.getParameters());
				Constructor[] cons = configClass.getConstructors();
				if(cons.length>0) {
					int min = 10;
					Constructor minC = null;
					for(Constructor con : cons) {
						if(con.getParameterCount()<min) {
							min = con.getParameterCount();
							minC = con;
						}
					}
					Type[] paramTypes = minC.getGenericParameterTypes();
					Object[] param = new Object[paramTypes.length];
					for(int i=0;i<paramTypes.length;i++) {
						AssemblyUtil tmp = new AssemblyUtil();
						if(paramTypes[i] instanceof ParameterizedType) {
							ParameterizedType  pType = (ParameterizedType) paramTypes[i];
							tmp.setTagClass((Class<?>) pType.getRawType());
							tmp.setClassGeneric(pType.getActualTypeArguments());
						}else {
							tmp.setTagClass((Class<?>) paramTypes[i]);
						}
						tmp.setBeanName(null);
						tmp.setNameMapTmp(assemblyData.getNameMapTmp());
//						if(paramTypes[i] instanceof ParameterizedType) {
//						}
//						log.info("AssemblyUtil factory=>{}",tmp.getTagClass());
						Object[] ojb_meth = ScanUtil.findCreateBeanFactoryClass(tmp);
//						log.debug("ojb_meth=>{}",ojb_meth);
						
						if(ojb_meth[0]!=null && ojb_meth[1] != null) {
							param[i] = buildBean((Class)ojb_meth[0],(Method)ojb_meth[1], tmp);
						}else {
							if(tmp.getClassGeneric()!=null) {
								param[i] = LazyBean.buildProxyForGeneric(tmp.getTagClass(),tmp.getClassGeneric());
							}else {
								param[i] = LazyBean.buildProxy(tmp.getTagClass());
							}
						}
					}
					
					factory.put(configClass, minC.newInstance(param));
				}else {
					cons = configClass.getDeclaredConstructors();
					if(!Modifier.isPublic(configClass.getModifiers())) {
						log.info("处理非公共类");
						cons[0].setAccessible(true);
					}
					if(cons.length>0) {
						factory.put(configClass, cons[0].newInstance());
					}
				}
				if(configClass.getAnnotation(ConfigurationProperties.class)!=null) {
					LazyConfigurationPropertiesBindingPostProcessor.processConfigurationProperties(factory.get(configClass));
				}
				LazyBean.processAttr(factory.get(configClass), configClass);
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			if(e.getCause()!=null) {
				log.error("构建Bean",e.getCause());
			}else {
				log.error("构建Bean",e);
			}
		}
		Object obj = factory.get(configClass);
		if(obj!=null) {
			try {
				Type[] paramTypes = method.getGenericParameterTypes();
				//如果存在参数
				Object[] args = new Object[paramTypes.length];
//				if(args.length>0) {
//					log.warn("存在二级Bean，需要处理");//
//				}
				for(int i=0;i<paramTypes.length;i++) {
					AssemblyUtil tmp = new AssemblyUtil();
					if(paramTypes[i] instanceof ParameterizedType) {
						ParameterizedType  pType = (ParameterizedType) paramTypes[i];
						tmp.setTagClass((Class<?>) pType.getRawType());
						tmp.setClassGeneric(pType.getActualTypeArguments());
					}else {
						tmp.setTagClass((Class<?>) paramTypes[i]);
					}
					tmp.setBeanName(null);
					tmp.setNameMapTmp(assemblyData.getNameMapTmp());
					
//					log.info("AssemblyUtil 2=>{}",tmp.getTagClass());
//					if(tmp.getTagClass().getName().contains("MongoClient")) {
//						log.info("断点");
//					}
					Object[] ojb_meth = ScanUtil.findCreateBeanFactoryClass(tmp);
//					log.info("ojb_meth=>{}",ojb_meth);
					
					if(ojb_meth[0]!=null && ojb_meth[1] != null) {
						args[i] = buildBean((Class)ojb_meth[0],(Method)ojb_meth[1], tmp);
						if(args[i] == null) {
							log.warn("arg 为空，警告");
						}
					}else {
						if(tmp.getClassGeneric()!=null) {
							args[i] = LazyBean.buildProxyForGeneric(tmp.getTagClass(),tmp.getClassGeneric());
						}else {
							args[i] = LazyBean.buildProxy(tmp.getTagClass());
						}
					}
				}
				Object tagObj = method.invoke(obj,args);
				ConfigurationProperties prop = null;
				if((prop = method.getAnnotation(ConfigurationProperties.class))!=null) {
//					String prefix = prop.value()!=""?prop.value():prop.prefix();
					LazyConfigurationPropertiesBindingPostProcessor.processConfigurationProperties(tagObj, prop);
				}
				cacheBean.put(key, tagObj);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("JavaBeanUtil#buildBean",e);
			}
		}
		return cacheBean.get(key);
	}
	/**
	 * 扫描java代码相关配置
	 */
	public static void process() {
		/**
		 * 处理数据库
		 */
		if(LazyMybatisMapperBean.useMybatis()) {
			List<Class<?>> configurableList = ScanUtil.findClassWithAnnotation(Configuration.class);
			configurableList.stream().filter(configura ->configura.getAnnotation(LazyMybatisMapperBean.getAnnotionClass())!=null).forEach(configura ->{
				Annotation scan = configura.getAnnotation(LazyMybatisMapperBean.getAnnotionClass());
				if(scan != null) {
					String[] packagePath = (String[]) InvokeUtil.invokeMethod(scan, "basePackages");
					if(packagePath.length>0) {
						LazyMybatisMapperBean.processConfig(configura,packagePath);
					}
				}
			});
		}
		/**
		 * 处理dubbo服务类
		 */
		if(LazyDubboBean.useDubbo()) {//加载到com.alibaba.dubbo.config.annotation.Service
			List<Class<?>> dubboServiceList = ScanUtil.findClassWithAnnotation(LazyDubboBean.getAnnotionClass());
			dubboServiceList.stream().forEach(dubboServiceClass ->{
				LazyDubboBean.putAnnService(dubboServiceClass);
			});
		}
	}

}
