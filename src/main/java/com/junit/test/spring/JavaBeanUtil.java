package com.junit.test.spring;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.google.common.collect.Maps;
import com.junit.test.LazyBean;
import com.junit.test.ScanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("rawtypes")
public class JavaBeanUtil {
	private static Map<Class,Object> factory = Maps.newHashMap();
	private static Map<String,Object> cacheBean = Maps.newHashMap();
	public static Object buildBean(Class<?> c, Method m, Class classBean, String beanName) {
		return buildBean(c, m, classBean, beanName, null);
	}
	public static Object buildBean(Class class1, Method method, Class classBean, String beanName,Map<String, Class> tmpBeanMap) {
		String key = classBean+"=>beanName:"+beanName;
		if(cacheBean.containsKey(key)) {
			return cacheBean.get(key);
		}
		try {
			if(!factory.containsKey(class1)) {
				Constructor[] cons = class1.getConstructors();
				if(cons.length>0) {
					int min = 10;
					Constructor minC = null;
					for(Constructor con : cons) {
						if(con.getParameterCount()<min) {
							min = con.getParameterCount();
							minC = con;
						}
					}
					Class[] paramTypes = minC.getParameterTypes();
					Object[] param = new Object[paramTypes.length];
					for(int i=0;i<paramTypes.length;i++) {
						Object[] ojb_meth = ScanUtil.findCreateBeanFactoryClass(paramTypes[i], null,tmpBeanMap);
						if(ojb_meth[0]!=null && ojb_meth[1] != null) {
							param[i] = buildBean((Class)ojb_meth[0],(Method)ojb_meth[1], paramTypes[i], null,tmpBeanMap);
						}else {
							param[i] = LazyBean.buildProxy(paramTypes[i]);
						}
					}
					
					factory.put(class1, minC.newInstance(param));
					LazyBean.processAttr(factory.get(class1), class1);
				}
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			if(e.getCause()!=null) {
				log.error("构建Bean",e.getCause());
			}else {
				log.error("构建Bean",e);
			}
		}
		Object obj = factory.get(class1);
		if(obj!=null) {
			try {
				Class[] paramTypes = method.getParameterTypes();
				//如果存在参数
				Object[] args = new Object[paramTypes.length];
				if(args.length>0) {
					log.warn("存在二级Bean，需要处理");//
				}
				for(int i=0;i<paramTypes.length;i++) {
					Object[] ojb_meth = ScanUtil.findCreateBeanFactoryClass(paramTypes[i], null,tmpBeanMap);
					if(ojb_meth[0]!=null && ojb_meth[1] != null) {
						args[i] = buildBean((Class)ojb_meth[0],(Method)ojb_meth[1], paramTypes[i], null,tmpBeanMap);
					}else {
						args[i] = LazyBean.buildProxy(paramTypes[i]);
					}
				}
				Object tagObj = method.invoke(obj,args);
				cacheBean.put(key, tagObj);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return cacheBean.get(key);
	}

}
