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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;

import com.alibaba.fastjson.JSONObject;
import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.model.AssemblyDTO;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.core.facade.scan.ClassScan;
import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazybean.beanfactory.AbstractLazyProxy;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;
import com.github.jklasd.test.lazyplugn.db.LazyMybatisMapperBean;
import com.github.jklasd.test.lazyplugn.db.LazyMybatisScannerRegistrar;
import com.github.jklasd.test.lazyplugn.dubbo.LazyDubboBean;
import com.github.jklasd.test.lazyplugn.spring.configprop.LazyConfPropBind;
import com.github.jklasd.test.util.AnnHandlerUtil;
import com.github.jklasd.test.util.BeanNameUtil;
import com.github.jklasd.test.util.CheckUtil;
import com.github.jklasd.test.util.JunitInvokeUtil;
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
	 * @param assemblyData 
	 * @return
	 */
    public Object buildBean(Class<?> configClass, Method method, AssemblyDTO assemblyData) {
	    if(StringUtils.isBlank(assemblyData.getBeanName())) {
	        assemblyData.setBeanName(BeanNameUtil.getBeanName(assemblyData.getTagClass()));
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
                log.warn("configClass=>{},method=>{},assemblyData=>{}",configClass.getSimpleName(),method.getName(),assemblyData);
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
    private void buildTagObject(Method method, AssemblyDTO assemblyData, String key, Object obj) {
        try {
        	if(AbstractLazyProxy.isProxy(obj)) {//不能是代理对象
        		return;
        	}
        	Object exitsBean = getExists(method);
        	if(exitsBean != null && !AbstractLazyProxy.isProxy(exitsBean)) {//不能是代理对象
        		log.info("---Bean 已构建,method:{}---",method);
        		cacheBean.put(key, exitsBean);
        		return;
        	}
        	//如果存在参数
        	Object[] args = buildParam(method.getGenericParameterTypes(),method.getParameterAnnotations());
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
        				args[i] = AbstractLazyProxy.getProxyTagObj(args[i]);
        			}
        		}
        		tagObj = method.invoke(obj,args);
        	}catch(Exception e) {
        		log.error("方法调用异常=>{}",method);
        		throw new JunitException(e);
        	}
        	
        	ConfigurationProperties prop = null;
        	if((prop = method.getAnnotation(ConfigurationProperties.class))!=null) {
        		LazyConfPropBind.processConfigurationProperties(tagObj, prop);
        	}
        	
        	if(assemblyData.getTagClass() == null) {
        		assemblyData.setTagClass(tagObj.getClass());
        	}
        	if(tagObj instanceof FactoryBean) {
        		FactoryBean fb = (FactoryBean) tagObj;
        		((InitializingBean) tagObj).afterPropertiesSet();
        		tagObj = fb.getObject();
        		cacheBean.put(key, tagObj);
        		TestUtil.getInstance().getApplicationContext().registBean(assemblyData.getBeanName(), tagObj, assemblyData.getTagClass());
        	}else {
        		cacheBean.put(key, tagObj);
        		TestUtil.getInstance().getApplicationContext().registBean(assemblyData.getBeanName(), tagObj, assemblyData.getTagClass());
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
            LazyBean.getInstance().processAttr(factory.get(configClass), configClass);
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
     * @param nameSpace 扫描域
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
    /**
     * 构建目标对象的方法参数对象
     * @param nameSpace 扫描域
     * @param paramTypes    参数类型组
     * @param annotations 
     * @return  参数对象组
     */
    private Object[] buildParam(Type[] paramTypes, Annotation[][] paramAnnotations) {
        Object[] param = new Object[paramTypes.length];
        for(int i=0;i<paramTypes.length;i++) {
        	AssemblyDTO tmp = new AssemblyDTO();
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
        		    }
        		}
        		Object obj = null;
        		if(tmp.getBeanName()==null) {
        		    obj = TestUtil.getInstance().getApplicationContext().getBeanByClass(tmp.getTagClass());
        		}else {
        		    obj = TestUtil.getInstance().getApplicationContext().getBean(tmp.getBeanName());
        		}
        		if(obj != null) {
        		    param[i] = obj;
        		    continue;
        		}
        	}
//        	tmp.setNameMapTmp(nameSpace);
        	Object[] ojb_meth = ScanUtil.findCreateBeanFactoryClass(tmp);
        	if(ojb_meth[0]!=null && ojb_meth[1] != null) {
        		param[i] = buildBean((Class)ojb_meth[0],(Method)ojb_meth[1], tmp);
        		if(param[i] == null) {
        		    log.warn("arg 为空，警告");
        		}
        	}else {
        		if(tmp.getClassGeneric()!=null) {
        			param[i] = LazyBean.getInstance().buildProxyForGeneric(tmp.getTagClass(),tmp.getClassGeneric());
        		}else {
        			param[i] = LazyBean.getInstance().buildProxy(tmp.getTagClass());
        		}
        	}
        }
        return param;
    }
}
