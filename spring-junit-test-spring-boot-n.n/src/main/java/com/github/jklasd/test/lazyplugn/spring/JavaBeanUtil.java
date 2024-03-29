package com.github.jklasd.test.lazyplugn.spring;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;

import com.alibaba.fastjson.JSONObject;
import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.interf.register.JunitCoreComponentI;
import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.model.JunitMethodDefinition;
import com.github.jklasd.test.common.util.AnnHandlerUtil;
import com.github.jklasd.test.common.util.CheckUtil;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazybean.beanfactory.LazyProxyManager;
import com.github.jklasd.test.lazyplugn.spring.configprop.ConfigurationModel;
import com.github.jklasd.test.lazyplugn.spring.configprop.LazyConfPropBind;
import com.github.jklasd.test.util.BeanNameUtil;
import com.github.jklasd.test.util.StackOverCheckUtil;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("rawtypes")
public class JavaBeanUtil {
	private Map<Class,Object> factory = Maps.newHashMap();
	private Map<String,Object> cacheBean = Maps.newHashMap();
	
	private JavaBeanUtil() {}
	private static JavaBeanUtil bean;
	public static JavaBeanUtil getInstance() {
	    if(bean == null) {
	        bean = new JavaBeanUtil();
	    }
	    return bean;
	}
	/**
	 * 构建目标对象
	 * 1、先构建configration对象
	 * 1-1 构建前先判断，构造器是否是公共的，判断是否需要参数传入，若有参数，则先去寻找并创建响应的参数对象
	 * 2、然后调用对应的方法去构建目标对象
	 * 2-1在构建目标对象前，先判断构建方法是否存在参数，若存在参数，则先去寻找并创建相应的参数对象
	 * @param configClass  构建目标对象的方法的类
	 * @param method   构建目标对象的方法
	 * @param assemblyData 构建参数 
	 * @return 返回构建的对象
	 */
    public Object buildBean(Class<?> configClass, Method method, BeanModel assemblyData) {
	    if(StringUtils.isBlank(assemblyData.getBeanName())) {
	        assemblyData.setBeanName(BeanNameUtil.getBeanNameForMethod(method,assemblyData.getTagClass()));
	    }
		String key = assemblyData.getTagClass()+"=>beanName:"+assemblyData.getBeanName();
		if(cacheBean.containsKey(key)) {
			return cacheBean.get(key);
		}
		
		if(!factory.containsKey(configClass)) {
		    /**
		     * 先创建 configuration BEAN
		     */
            boolean buildConfigStatus = buildConfigObj(configClass,null);
            if(buildConfigStatus) {
                log.info("configClass=>{},method=>{},assemblyData=>{}",configClass.getSimpleName(),method.getName(),assemblyData);
            }
        }
		
		Object obj = factory.get(configClass);
		if(obj!=null) {   //若configuration对象存在，则开始创建目标对象
			buildTagObject(method, assemblyData, key, obj);
		}
		
		return cacheBean.get(key);
	}
    
    public Object getExists(Method method) {
    	return StackOverCheckUtil.exec(()->{
    		//查看是否已经执行过来
        	Bean aw = method.getAnnotation(Bean.class);
            String beanName = null;
            if(aw.value().length>0) {
            	beanName = aw.value()[0];
            }else if(aw.name().length>0){
            	beanName = aw.name()[0];
            }else {
            	beanName = method.getName();
            }
            if(TestUtil.getInstance().getApplicationContext().containsBean(beanName)) {
            	return TestUtil.getInstance().getApplicationContext().getBean(beanName);
            }
            return null;
    	},method);
    }
    
    /**
     * 构建目标对象
     * @param method    构建目标对象
     * @param assemblyData  传输data
     * @param key   缓存key
     * @param obj   configurationBean
     */
    private void buildTagObject(Method method, BeanModel assemblyData, String key, Object obj) {
        try {
        	if(LazyProxyManager.isProxy(obj)) {//不能是代理对象
        		return;
        	}
        	Object exitsBean = getExists(method);
        	if(exitsBean != null && !LazyProxyManager.isProxy(exitsBean)) {//不能是代理对象
        		log.info("---Bean 已构建,method:{}---",method);
        		cacheBean.put(key, exitsBean);
        		return;
        	}
//        	if(method.getName().contains("compositeDiscoveryClient")) {
//    			log.debug("断点");
//    		}
        	//如果存在参数
        	Object[] args = buildParam(method.getGenericParameterTypes(),method.getParameterAnnotations(),assemblyData);
        	if(!method.isAccessible()) {
        		method.setAccessible(true);
        	}
        	Object tagObj = null;
        	try {
        		/*
        		 * 注入对象尽量采用真实对象，防止个别特殊设计问题
        		 * 比如强转类型，如果是代理对象强转成个别设计的类型，会导致异常
        		 */
        		for(int i=0;i<args.length;i++) {
        			if(args[i] instanceof Proxy) {
        				args[i] = LazyProxyManager.getProxyTagObj(args[i]);
        			}
        		}
        		tagObj = method.invoke(obj,args);
        	}catch(Exception e) {
        		log.error("方法调用异常=>{},err:{}",method,e.getMessage());
        		throw new JunitException(e);
        	}
        	
        	ConfigurationProperties prop = null;
        	if((prop = method.getAnnotation(ConfigurationProperties.class))!=null
        			|| (prop = tagObj.getClass().getAnnotation(ConfigurationProperties.class))!=null) {
        		ConfigurationModel cm = new ConfigurationModel();
        		cm.setMethod(method);
        		cm.setObj(tagObj);
        		cm.setProp(prop);
        		LazyConfPropBind.processConfigurationProperties(cm);
        	}
        	
        	if(assemblyData.getTagClass() == null) {
        		assemblyData.setTagClass(tagObj.getClass());
        	}
        	if(tagObj instanceof FactoryBean) {
        		FactoryBean fb = (FactoryBean) tagObj;
        		((InitializingBean) tagObj).afterPropertiesSet();
        		tagObj = fb.getObject();
        		cacheBean.put(key, tagObj);
//        		TestUtil.getInstance().getApplicationContext().registProxyBean(assemblyData.getBeanName(), tagObj, assemblyData.getTagClass());
        		LazyListableBeanFactory.getInstance().registerSingleton(assemblyData.getBeanName(), tagObj);
        	}else {
        		cacheBean.put(key, tagObj);
//        		TestUtil.getInstance().getApplicationContext().registProxyBean(assemblyData.getBeanName(), tagObj, assemblyData.getTagClass());
        		LazyListableBeanFactory.getInstance().registerSingleton(assemblyData.getBeanName(), tagObj);
        	}
        } catch (Exception e) {
        	log.error("JavaBeanUtil#buildBean=>{},obj:{},method:{}",JSONObject.toJSON(assemblyData),obj,method);
        	throw new JunitException(e);
        }
    }
    private boolean buildConfigObj(Class<?> configClass, Map<String,Class<?>> nameSpace) {
    	
    	if(!CheckUtil.checkClassExists(configClass)) {
    		return false;
    	}
    	
        try {
        	Map<String, Object> annoData = AnnHandlerUtil.getInstance().getAnnotationValue(configClass,AutoConfigureAfter.class);
            if(annoData!=null && !annoData.isEmpty()) {//先加载另外一个类
            	String[] classArr = (String[]) annoData.get("value");
            	for(String className : classArr) {
            		Class<?> tmp = ScanUtil.loadClass(className);
            		if(tmp!=null) {
            			buildConfigObj(tmp,nameSpace);
            		}
            	}
            }
            PropertySource propSrouce = configClass.getAnnotation(PropertySource.class);
            if(propSrouce!=null) {//先加载配置
            	try {
            		for(String path : propSrouce.value()) {
            			Resource propRes = ScanUtil.getRecourceAnyOne(path);
            			if(propRes!=null && propRes.exists()) {
            				Properties properties = new Properties();
            				properties.load(propRes.getInputStream());
            				TestUtil.getInstance().getApplicationContext().getEnvironment().getPropertySources()
            				.addLast(new PropertiesPropertySource(propSrouce.name(), properties));
            			}
            		}
				} catch (Exception e) {
				}
            }
            Constructor[] cons = configClass.getConstructors();
            if(cons.length>0) {
                findAndCreateBean(configClass, cons);
            }else {
                cons = configClass.getDeclaredConstructors();
                if(!Modifier.isPublic(configClass.getModifiers())) {
                    cons[0].setAccessible(true);
                }
                if(cons.length>0) {
                    findAndCreateBean(configClass, cons);
                }
            }
            if(configClass.getAnnotation(ConfigurationProperties.class)!=null) {
            	LazyConfPropBind.processConfigurationProperties(factory.get(configClass));
            }
//            LazyBean.getInstance().processAttr(factory.get(configClass), configClass);
            BeanInitModel initModel = new BeanInitModel();
			initModel.setObj(factory.get(configClass));
			initModel.setTagClass(configClass);
			initModel.setBeanName(configClass.getName());
    		LazyBean.getInstance().processAttr(initModel);// 递归注入代理对象
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | IOException e) {
            log.error("构建Configuration Bean=>{}",configClass.getSimpleName());
            log.error("构建Bean",e);
            return false;
        }
        return true;
    }
    /**
     * 查询对应的构造函数，并创建bean
     * @param configClass   confirguration对象
     * @param cons  构建函数
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void findAndCreateBean(Class<?> configClass, Constructor[] cons)
        throws InstantiationException, IllegalAccessException, InvocationTargetException {
        int min = 10;
        Constructor minC = null;
        for(Constructor con : cons) {
        	if(con.getParameterCount()<min) {
        		min = con.getParameterCount();
        		minC = con;
        	}
        }
        //构建参数
        Object[] param = buildParam(minC.getGenericParameterTypes(),minC.getParameterAnnotations());
        //创建对象并缓存
        factory.put(configClass, minC.newInstance(param));
    }
    public Object[] buildParam(Type[] paramTypes, Annotation[][] paramAnnotations,BeanModel assemblyData) {
        Object[] param = new Object[paramTypes.length];
        for(int i=0;i<paramTypes.length;i++) {
        	BeanModel tmp = new BeanModel();
        	tmp.setBeanName(null);
        	if(paramTypes[i] instanceof ParameterizedType) {
        		ParameterizedType  pType = (ParameterizedType) paramTypes[i];
        		tmp.setTagClass((Class<?>) pType.getRawType());
        		tmp.setClassGeneric(pType.getActualTypeArguments());
        	}else {
        		tmp.setTagClass((Class<?>) paramTypes[i]);
        		for(Annotation ann :paramAnnotations[i]) {
        		    if(ann.annotationType() == Qualifier.class) {
        		        tmp.setBeanName(((Qualifier)ann).value());
        		        break;
        		    }else if(ann.annotationType() == Value.class) {
        		    	TestUtil util = ContainerManager.getComponent(JunitCoreComponentI.class.getSimpleName());
        		    	param[i] = util.valueFromEnvForAnnotation(((Value)ann).value(), tmp.getTagClass());
        		    }else if(ann.annotationType() == Autowired.class) {
        		    	Autowired aw = (Autowired) ann;
        		    	if(!aw.required()) {
        		    		tmp.setRequired(false);
        		    	}
        		    }
        		}
        		if(param[i]!=null) {
        			continue;
        		}
        		Object obj = null;
        		if(tmp.getBeanName()==null) {
        		    obj = TestUtil.getInstance().getApplicationContext().getProxyBeanByClass(tmp.getTagClass());
        		}else {
        		    obj = TestUtil.getInstance().getApplicationContext().getBean(tmp.getBeanName());
        		}
        		if(obj != null) {
        		    param[i] = obj;
        		    continue;
        		}
        	}
//        	tmp.setNameMapTmp(nameSpace);
        	JunitMethodDefinition jmd = ScanUtil.findCreateBeanFactoryClass(tmp);
        	if(jmd!=null) {
        		/**
        		 * 查看是否已经存在
        		 */
        		
        		
        		param[i] = buildBean(jmd.getConfigurationClass(),jmd.getMethod(), tmp);
        		if(param[i] == null) {
        		    log.warn("arg 为空，警告");
        		}
        	}else {
        		if(tmp.getClassGeneric()!=null) {
        			String exculdeName = assemblyData!=null?assemblyData.getBeanName():tmp.getBeanName();
        			param[i] = LazyBean.getInstance().buildProxyForGeneric(tmp.getTagClass(),tmp.getClassGeneric(),exculdeName);
        		}else {
        			if(tmp.getBeanName()==null) {
        				tmp.setBeanName(BeanNameUtil.fixedBeanName(tmp.getTagClass()));
        			}
        			param[i] = LazyBean.getInstance().buildProxy(tmp);
        		}
        	}
        }
        return param;
    }
    /**
     * 构建目标对象的方法参数对象
     * @param paramTypes    参数类型组
     * @param paramAnnotations    参数的注解组
     * @return  参数对象组
     */
    public Object[] buildParam(Type[] paramTypes, Annotation[][] paramAnnotations) {
        return buildParam(paramTypes, paramAnnotations, null);
    }
}
