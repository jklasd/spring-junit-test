package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.interf.DatabaseInitialization;
import com.github.jklasd.test.common.interf.handler.LazyPlugnBeanFactory;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.LazyDataSourceUtil;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory.RootBeanDefinitionBuilder;
import com.github.jklasd.test.util.JunitInvokeUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author jubin.zhang
 *
 */
@Slf4j
public class LazyMybatisMapperBean extends LazyAbstractPlugnBeanFactory implements LazyPlugnBeanFactory{
	public static LazyAbstractPlugnBeanFactory getInstance() {
		return getInstanceByClass(LazyMybatisMapperBean.class);
	}
	@Override
	public String getName() {
		return "LazyMybatisMapperBean";
	}
	
	@Override
	public boolean isInterfaceBean() {
		return true;
	}
	
	@Override
	public Integer getOrder() {
		return 100;
	}

    private Object defaultFactory;

    private static final Class<?> factoryClass = ScanUtil.loadClass("org.apache.ibatis.session.SqlSessionFactory");
    private final Class<?> MapperFactoryBeanClass = ScanUtil.loadClass("org.mybatis.spring.mapper.MapperFactoryBean");

    @Override
	public boolean canBeInstance() {
		return MapperFactoryBeanClass!=null;
	}
    
    public static final boolean useMybatis() {
        return factoryClass != null;
    }

    private void restDataSource() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException{
    	DatabaseInitialization dataInitia = ContainerManager.getComponent(DatabaseInitialization.class.getName());
    	if(dataInitia!=null) {
	    	Object configuration = JunitInvokeUtil.invokeMethod(defaultFactory, "getConfiguration");
	    	Object dataBaseEnv = JunitInvokeUtil.invokeMethod(configuration, "getEnvironment");
	    	if(dataBaseEnv!=null) {
//	    		System.out.println(dataBaseEnv);
	    		DataSource dataSource = (DataSource) JunitInvokeUtil.invokeMethod(dataBaseEnv, "getDataSource");
	    		LazyDataSourceUtil.defaultDataSouce(dataSource);
	    		
	    		Object newDataSource = dataInitia.build(dataSource);
	    		if(newDataSource != null) {
	    			Class<?> EnvironmentClass = ScanUtil.loadClass("org.apache.ibatis.mapping.Environment");
	    			Class<?> TransactionFactory = ScanUtil.loadClass("org.apache.ibatis.transaction.TransactionFactory");
	    			Object newDataBaseEnv = EnvironmentClass.getConstructor(String.class,TransactionFactory,DataSource.class)
	    			.newInstance(JunitInvokeUtil.invokeMethod(dataBaseEnv,"getId"),
	    					JunitInvokeUtil.invokeMethod(dataBaseEnv,"getTransactionFactory"),
	    					newDataSource);
	    			JunitInvokeUtil.invokeMethod(configuration, "setEnvironment",newDataBaseEnv);
	    			
	    			Object SqlInterceptor = ContainerManager.getComponent(ContainerManager.NameConstants.SqlInterceptor);
	    			if(SqlInterceptor!=null) {
	    				Object plugn = JunitInvokeUtil.invokeMethod(SqlInterceptor, "buildInterceptor");
	    				if(plugn!=null) {
	    					JunitInvokeUtil.invokeMethodByParamClass(configuration, "addInterceptor",new Class<?>[] {ScanUtil.loadClass("org.apache.ibatis.plugin.Interceptor")},new Object[] {plugn});
	    				}
	    			}
	    		}
	    	}
    	}
	}

	private Object getMapper(Class<?> classBean) throws Exception {
		BeanDefinition beanDef = mapperBeanDef.get(classBean.getName());
//        Object tag = JunitInvokeUtil.invokeMethod(getSqlSessionTemplate(), "getMapper", classBean);
        return LazyListableBeanFactory.getInstance().doCreateBean(classBean.getName(), RootBeanDefinitionBuilder.build(beanDef), null);
    }


    private static List<String> mybatisScanPathList = Lists.newArrayList();

    private Class<?> mapperScannerConfigurer
        = ScanUtil.loadClass("org.mybatis.spring.mapper.MapperScannerConfigurer");
//    private static boolean loadScaned;
    
    private static Map<String,Object> mappingSession = Maps.newHashMap();
    
    public synchronized void processConfig(Class<?> configura, List<String> basePackages) {
        mybatisScanPathList.addAll(Sets.newConcurrentHashSet(basePackages));
    }

    @Override
    public Object buildBean(BeanModel model) {
        Class<?> tagC = model.getTagClass();
        try {
            return getMapper(tagC);
        } catch (Exception e) {
            log.error("获取Mapper", e);
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
	volatile int inited;
	@Override
	public boolean finded(BeanModel beanModel) {
		if(beanModel.getTagClass() == null) {
			return false;
		}
		try {
			if(inited<3) {
				if(inited<2) {
					initMybatisBasePackage();
				}
				if(inited<3) {
					boolean finded = !mybatisScanPathList.isEmpty() && mybatisScanPathList.stream()
			        .anyMatch(mybatisScanPath -> {
			        	boolean tmp = beanModel.getTagClass().getPackage().getName().contains(mybatisScanPath);
			        	return tmp;
			        });
			        if(finded) {
			        		initMybatis();
			        }
				}
			}
		}catch (Exception e) {
			log.error("初始化失败",e);
			return false;
		}
		return mapperBeanDef.containsKey(beanModel.getTagClass().getName());
	}
	
	private void initMybatisBasePackage() {
		if (inited>1) {
			return;
		}
		inited = 2;
		Object mapperScan = null;
		try {
			mapperScan = LazyListableBeanFactory.getInstance().getBean(mapperScannerConfigurer);
		} catch (BeansException e) {
			log.warn("未找到{}相关bean",mapperScannerConfigurer);
			return;
		}
		String basePackage = (String) JunitInvokeUtil.invokeReadField("basePackage", mapperScan);
		if(basePackage!=null) {
    		mybatisScanPathList.add(basePackage);
    		log.info("mybatisScanPathList=>{}", mybatisScanPathList);
    	}
	}

	Map<String,BeanDefinition> mapperBeanDef = Maps.newHashMap();
	
	private synchronized void initMybatis() {
		if (inited>2) {
			return;
		}
		inited = 3;
		Object mapperScan = null;
		try {
			mapperScan = LazyListableBeanFactory.getInstance().getBean(mapperScannerConfigurer);
		} catch (BeansException e) {
			log.warn("未找到{}相关bean",mapperScannerConfigurer);
			return;
		}
		
		String sqlSessionFactoryBeanName = (String) JunitInvokeUtil.invokeReadField("sqlSessionFactoryBeanName", mapperScan);
		defaultFactory = LazyListableBeanFactory.getInstance().getBean(sqlSessionFactoryBeanName);
		
		handMapperBeanDefinition(mapperScan);
		
		try {
			restDataSource();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new JunitException("数据源 重置失败", true);
		}
	}
	/**
	 * 待优化
	 * 暂时扫描出所有mapperbeanDefinition
	 * @param mapperScan mapperScanBean
	 */
	private void handMapperBeanDefinition(Object mapperScan) {
		BeanDefinitionRegistryPostProcessor postProcessor = (BeanDefinitionRegistryPostProcessor)mapperScan;
		JunitInvokeUtil.invokeWriteField("sqlSessionFactory",  mapperScan ,defaultFactory);
		postProcessor.postProcessBeanDefinitionRegistry(LazyListableBeanFactory.getInstance());
		
		String[] beanNames = LazyListableBeanFactory.getInstance().getBeanNamesForType(MapperFactoryBeanClass);
		for(String beanName : beanNames) {
			BeanDefinition beanDef = LazyListableBeanFactory.getInstance().getBeanDefinition(beanName);
			if(beanDef instanceof ScannedGenericBeanDefinition) {
				ScannedGenericBeanDefinition tmp = (ScannedGenericBeanDefinition) beanDef;
				AnnotationMetadata metaData = tmp.getMetadata();
				mapperBeanDef.put(metaData.getClassName(), beanDef);
			}
			LazyListableBeanFactory.getInstance().removeBeanDefinition(beanName);
		}
	}

}
