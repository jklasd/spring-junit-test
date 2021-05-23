package com.github.jklasd.test.db;

import java.util.Map;

import com.github.jklasd.test.AssemblyUtil;
import com.github.jklasd.test.LazyBeanFactory;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.beanfactory.LazyBean;
import com.github.jklasd.test.beanfactory.LazyProxy;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
public class LazyMongoBean implements LazyBeanFactory{
	private final static String MONGO_PATH = "springframework.data.mongodb";
	public static boolean isMongo(Class c) {
		return c.getPackage().getName().contains(MONGO_PATH);
	}

	private static Map<Class,Object> cacheBean = Maps.newHashMap();
	
	public static Object buildBean(Class classBean, String beanName){
		if(cacheBean.containsKey(classBean)) {
			return cacheBean.get(classBean);
		}
		AssemblyUtil asse = new AssemblyUtil();
		asse.setTagClass(classBean);
		asse.setBeanName(beanName);
		asse.setNameMapTmp(ScanUtil.findClassMap(ScanUtil.BOOT_AUTO_CONFIG));
		Object obj = LazyBean.findCreateBeanFromFactory(asse);
//		log.info("obj=>{}",obj!=null);
		/**
		 * DefaultListableBeanFactory
		 * org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
		 * org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
		 */
		return obj;
	}

    @Override
    public Object buildBean(LazyProxy model) {
        Class tagC = model.getBeanModel().getTagClass();
        if(isMongo(tagC)) {
            return buildBean(tagC, model.getBeanModel().getBeanName());
        }
         return null;
    }
}
