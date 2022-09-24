package com.github.jklasd.test.lazyplugn.db;

import java.util.Map;

import com.github.jklasd.test.common.model.AssemblyDTO;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.BaseAbstractLazyProxy;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
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
    public Object buildBean(BeanModel model) {
        Class tagC = model.getTagClass();
        if(isMongo(tagC)) {
            return buildBean(tagC, model.getBeanName());
        }
         return null;
    }

	@Override
	public boolean finded(BeanModel beanModel) {
		// TODO Auto-generated method stub
		return false;
	}
}
