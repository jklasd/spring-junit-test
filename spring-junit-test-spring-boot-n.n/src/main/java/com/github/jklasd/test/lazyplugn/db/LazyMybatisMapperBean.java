package com.github.jklasd.test.lazyplugn.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.model.AssemblyDTO;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazybean.beanfactory.AbstractLazyProxy;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.util.JunitInvokeUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang
 *
 */
@Slf4j
public class LazyMybatisMapperBean implements LazyPlugnBeanFactory{
    private static volatile LazyMybatisMapperBean bean;

    public static LazyMybatisMapperBean getInstance() {
        if (bean == null) {
            bean = new LazyMybatisMapperBean();
        }
        return bean;
    }

    private static Object defaultFactory;

    public synchronized Object buildBean(Class<?> classBean) {
        try {
            return getMapper(classBean);
        } catch (Exception e) {
            log.error("获取Mapper", e);
        }
        return null;
    }

    // private static ThreadLocal<SqlSession> sessionList = new ThreadLocal<>();
    private static final Class<?> factoryClass = ScanUtil.loadClass("org.apache.ibatis.session.SqlSessionFactory");
//    private static final Class<?> factoryBeanClass = ScanUtil.loadClass("org.mybatis.spring.SqlSessionFactoryBean");
    private static final Class<?> sqlSessionTemplateClass = ScanUtil.loadClass("org.mybatis.spring.SqlSessionTemplate");

    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation> mapperScanClass
        = ScanUtil.loadClass("org.mybatis.spring.annotation.MapperScan");

    public static final boolean useMybatis() {
        return factoryClass != null;
    }

    private Object sqlSessionTemplate;
    private Map<String,Object> sqlSessionTemplateMaps = Maps.newHashMap();

    public Object getSqlSessionTemplate() throws Exception {
        if (defaultFactory == null) {
            buildMybatisFactory();
        }
        if(sqlSessionTemplateMaps.containsKey(mappingPath.get())) {
        	return sqlSessionTemplateMaps.get(mappingPath.get());
        }
        
        if (sqlSessionTemplateClass != null && sqlSessionTemplate == null) {// 配置session控制器
            Constructor<?> structor = sqlSessionTemplateClass.getConstructor(factoryClass);
            Object factory = mappingSession.get(mappingPath.get());
            factory = factory==null?defaultFactory:factory;
            sqlSessionTemplateMaps.put(mappingPath.get(), structor.newInstance(factory));
        }
        return sqlSessionTemplateMaps.get(mappingPath.get());
    }

    private Object getMapper(Class<?> classBean) throws Exception {
        Object tag = JunitInvokeUtil.invokeMethod(getSqlSessionTemplate(), "getMapper", classBean);
        return tag;
    }

    private void buildMybatisFactory() {
        if (defaultFactory == null) {
            Object obj = TestUtil.getInstance().getApplicationContext().getBeanByClass(factoryClass);
            if (obj != null) {
                defaultFactory = obj;
                return;
            }
            processAnnaForFactory();
        } else {
            log.debug("factory已存在");
        }
    }

    private void processAnnaForFactory() {
        if (defaultFactory == null) {
            AssemblyDTO param = new AssemblyDTO();
            param.setTagClass(factoryClass);
            defaultFactory = LazyBean.findCreateBeanFromFactory(param);
        }
    }

    private static List<String> mybatisScanPathList = Lists.newArrayList();

    private static Class<?> mapperScannerConfigurer
        = ScanUtil.loadClass("org.mybatis.spring.mapper.MapperScannerConfigurer");
    private static boolean loadScaned;
    
    private static Map<String,Object> mappingSession = Maps.newHashMap();
    
    private static ThreadLocal<String> mappingPath = new ThreadLocal<String>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean isMybatisBean(Class c) {
    	if (useMybatis() && !loadScaned) {
            Object mybatisScan = TestUtil.getInstance().getApplicationContext().getBeanByClass(mapperScannerConfigurer);
            try {
            	String basePackage = null;
            	if(mybatisScan != null && AbstractLazyProxy.isProxy(mybatisScan)) {
        			AbstractLazyProxy obj = AbstractLazyProxy.getProxy(mybatisScan);
            		if (obj.getAttr().containsKey("basePackage")) {
            			basePackage = obj.getAttr().get("basePackage").toString();
            		}
            	}
            	if(basePackage!=null) {
            		mybatisScanPathList.add(basePackage);
            		log.info("mybatisScanPathList=>{}", mybatisScanPathList);
            	}
            } catch (Exception e) {
                e.printStackTrace();
            }
            loadScaned = true;
        }
        Class mapperAnn = ScanUtil.loadClass("org.apache.ibatis.annotations.Mapper");
        if (mapperAnn != null) {
            if (c.getAnnotation(mapperAnn) != null) {
                return true;
            }
        }
        
        AtomicReference<String> scanPath = new AtomicReference<String>();
        boolean finded = !mybatisScanPathList.isEmpty() && mybatisScanPathList.stream()
        .anyMatch(mybatisScanPath -> {
        	boolean tmp = c.getPackage().getName().contains(mybatisScanPath);
        	if(tmp) {
        		scanPath.set(mybatisScanPath);
        	}
        	return tmp;
        });
        if(finded) {
        	mappingPath.set(scanPath.get());
        }
        return finded;
    }

    public synchronized void processConfig(Class<?> configura, List<String> basePackages) {
        mybatisScanPathList.addAll(basePackages);
    }

    public void configure() {
        // 判断是否存在类
//        Class<?> abstractRoutingDataSource
//            = ScanUtil.loadClass("org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource");
//        LazyBeanProcess.putAfterMethodEvent(abstractRoutingDataSource);
    }

    @Override
    public Object buildBean(AbstractLazyProxy model) {
        Class<?> tagC = model.getBeanModel().getTagClass();
        if(isMybatisBean(tagC)) {
            try {
                return getMapper(tagC);
            } catch (Exception e) {
                log.error("获取Mapper", e);
            }
        }
        return null;
    }

	public void processScannerConfig() {
		Object obj = LazyBean.findCreateBeanFromFactory(mapperScannerConfigurer, null);
		if(obj!=null) {
			try {
				register(obj);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void register(Object obj) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InterruptedException {//MapperScannerConfigurer
		//强行拿取数据
		Field tmp = mapperScannerConfigurer.getDeclaredField("basePackage");
		if(!tmp.isAccessible()) {
			tmp.setAccessible(true);
		}
		//basePackage
		String basePackageValue = JunitInvokeUtil.getField(tmp, obj).toString();
		//sqlSessionFactoryBeanName
		tmp = mapperScannerConfigurer.getDeclaredField("sqlSessionFactoryBeanName");
		if(!tmp.isAccessible()) {
			tmp.setAccessible(true);
		}
		String sqlSessionFactoryBeanName = JunitInvokeUtil.getField(tmp, obj).toString();
		mybatisScanPathList.add(basePackageValue);
		mappingSession.put(basePackageValue,LazyBean.findCreateBeanFromFactory(factoryClass, sqlSessionFactoryBeanName));
	}
}