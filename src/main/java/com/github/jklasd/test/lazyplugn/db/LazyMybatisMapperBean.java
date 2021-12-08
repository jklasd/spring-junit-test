package com.github.jklasd.test.lazyplugn.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.beanfactory.LazyCglib;
import com.github.jklasd.test.lazybean.model.AssemblyDTO;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.util.InvokeUtil;
import com.github.jklasd.test.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.AbastractLazyProxy;
import com.google.common.collect.Lists;

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

    private static Object factory;

    public synchronized Object buildBean(Class<?> classBean) {
        try {
            return getMapper(classBean);
        } catch (Exception e) {
            log.error("获取Mapper", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation> mapperScanClass
        = ScanUtil.loadClass("org.mybatis.spring.annotation.MapperScan");

    public static final boolean useMybatis() {
        return factoryClass != null;
    }

    public static final Class<? extends Annotation> getAnnotionClass() {
        if (mapperScanClass != null) {
            return mapperScanClass;
        }
        return null;
    }

    // private static ThreadLocal<SqlSession> sessionList = new ThreadLocal<>();
    private static final Class<?> factoryClass = ScanUtil.loadClass("org.apache.ibatis.session.SqlSessionFactory");
//    private static final Class<?> factoryBeanClass = ScanUtil.loadClass("org.mybatis.spring.SqlSessionFactoryBean");
    private static final Class<?> sqlSessionTemplateClass = ScanUtil.loadClass("org.mybatis.spring.SqlSessionTemplate");

    private Object sqlSessionTemplate;

    public Object getSqlSessionTemplate() throws Exception {
        if (factory == null) {
            buildMybatisFactory();
        }
        if (sqlSessionTemplateClass != null && sqlSessionTemplate == null) {// 配置session控制器
            Constructor<?> structor = sqlSessionTemplateClass.getConstructor(factoryClass);
            sqlSessionTemplate = structor.newInstance(factory);
        }
        return sqlSessionTemplate;
    }

    private Object getMapper(Class<?> classBean) throws Exception {
        Object tag = InvokeUtil.invokeMethod(getSqlSessionTemplate(), "getMapper", classBean);
        return tag;
    }

    private void buildMybatisFactory() {
        if (factory == null) {
            Object obj = TestUtil.getInstance().getApplicationContext().getBeanByClass(factoryClass);
                if (obj != null) {
                    factory = obj;
                    return;
                }
//            }
            processAnnaForFactory();
        } else {
            log.debug("factory已存在");
        }
    }

    private void processAnnaForFactory() {
        if (factory == null) {
            AssemblyDTO param = new AssemblyDTO();
            param.setTagClass(factoryClass);
            factory = LazyBean.findCreateBeanFromFactory(param);
        }
    }

    private static List<String> mybatisScanPathList = Lists.newArrayList();

    private static Class<?> mapperScannerConfigurer
        = ScanUtil.loadClass("org.mybatis.spring.mapper.MapperScannerConfigurer");
    private static boolean loadScaned;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean isMybatisBean(Class c) {
        Class mapperAnn = ScanUtil.loadClass("org.apache.ibatis.annotations.Mapper");
        if (mapperAnn != null) {
            if (c.getAnnotation(mapperAnn) != null) {
                return true;
            }
        }
        return !mybatisScanPathList.isEmpty() && mybatisScanPathList.stream()
            .anyMatch(mybatisScanPath -> c.getPackage().getName().contains(mybatisScanPath));
    }

    public synchronized void processConfig(Class<?> configura, String[] packagePath) {
        mybatisScanPathList.addAll(Lists.newArrayList(packagePath));
    }

    public void configure() {
        // 判断是否存在类
//        Class<?> abstractRoutingDataSource
//            = ScanUtil.loadClass("org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource");
//        LazyBeanProcess.putAfterMethodEvent(abstractRoutingDataSource);
    }

    @Override
    public Object buildBean(AbastractLazyProxy model) {
    	if (useMybatis() && !loadScaned) {
            Object mybatisScan = TestUtil.getInstance().getApplicationContext().getBeanByClass(mapperScannerConfigurer);
            try {
            	String basePackage = null;
            	if(mybatisScan != null) {
            		Field cglibObjField = mybatisScan.getClass().getDeclaredField(LazyBean.PROXY_BEAN_FIELD);
            		cglibObjField.setAccessible(true);
            		LazyCglib obj = (LazyCglib)cglibObjField.get(mybatisScan);
            		if (obj.getAttr().containsKey("basePackage")) {
            			basePackage = obj.getAttr().get("basePackage").toString();
            		}
            	}
            	if(basePackage!=null) {
            		mybatisScanPathList.add(basePackage);
            		log.info("mybatisScanPathList=>{}", mybatisScanPathList);
            	}
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                e.printStackTrace();
            }
            loadScaned = true;
        }
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

}