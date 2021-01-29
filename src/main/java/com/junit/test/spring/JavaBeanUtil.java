package com.junit.test.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.google.common.collect.Maps;
import com.junit.test.LazyBean;
import com.junit.test.ScanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavaBeanUtil {
	private static Map<Class,Object> factory = Maps.newHashMap();
	private static Map<String,Object> cacheBean = Maps.newHashMap();
	public static Object buildBean(Class<?> c, Method m, Class classBean, String beanName) {
		String key = classBean+"=>beanName:"+beanName;
		if(cacheBean.containsKey(key)) {
			return cacheBean.get(key);
		}
		try {
			if(!factory.containsKey(c)) {
				factory.put(c, c.newInstance());
				LazyBean.processAttr(factory.get(c), c);
			}
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		//configration
		Object obj = factory.get(c);
		if(obj!=null) {
			try {
				Class[] paramTypes = m.getParameterTypes();
				//如果存在参数
				Object[] args = new Object[paramTypes.length];
				if(args.length>0) {
					log.warn("存在二级Bean，需要处理");//
				}
				for(int i=0;i<paramTypes.length;i++) {
					Object[] ojb_meth = ScanUtil.findCreateBeanFactoryClass(paramTypes[i], null);
					if(ojb_meth[0]!=null && ojb_meth[1] != null) {
						args[i] = buildBean((Class)ojb_meth[0],(Method)ojb_meth[1], paramTypes[i], null);
					}
				}
				Object tagObj = m.invoke(obj);
				cacheBean.put(key, tagObj);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return cacheBean.get(key);
	}

}
