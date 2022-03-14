package com.github.jklasd.test.lazyplugn.db;

import java.util.Map;

import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.model.AssemblyDTO;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.AbstractLazyProxy;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
public class LazyMongoBean implements LazyPlugnBeanFactory{
	private final static String MONGO_PATH = "springframework.data.mongodb";
	public static boolean isMongo(Class c) {
		return c.getPackage().getName().contains(MONGO_PATH);
	}

	private static Map<Class,Object> cacheBean = Maps.newHashMap();
	
	public static Object buildBean(Class classBean, String beanName){
		if(cacheBean.containsKey(classBean)) {
			return cacheBean.get(classBean);
		}
		AssemblyDTO asse = new AssemblyDTO();
		asse.setTagClass(classBean);
		asse.setBeanName(beanName);
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
    public Object buildBean(AbstractLazyProxy model) {
        Class tagC = model.getBeanModel().getTagClass();
        if(isMongo(tagC)) {
            return buildBean(tagC, model.getBeanModel().getBeanName());
        }
         return null;
    }
}
