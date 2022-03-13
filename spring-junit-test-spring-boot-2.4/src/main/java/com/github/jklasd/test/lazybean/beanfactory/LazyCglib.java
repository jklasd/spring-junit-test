package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazybean.model.BeanModel;
import com.github.jklasd.test.lazyplugn.db.LazyMongoBean;
import com.github.jklasd.test.lazyplugn.dubbo.LazyDubboBean;
import com.github.jklasd.test.lazyplugn.spring.configprop.LazyConfPropBind;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
import com.github.jklasd.test.util.ScanUtil;
import com.github.jklasd.test.util.StackOverCheckUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyCglib extends AbstractLazyProxy implements MethodInterceptor {
    @Getter
    private Constructor<?> constructor;
    public LazyCglib(BeanModel beanModel) {
        super(beanModel);
        setConstructor();
        initLazyProxy();
    }
    
    public boolean hasFinalMethod() {
        Method[] ms = beanModel.getTagClass().getDeclaredMethods();
        for(Method m : ms) {
            if(Modifier.isFinal(m.getModifiers())
                    && Modifier.isPublic(m.getModifiers())) {
                //存在final方法，且是公共方法，不能使用代理对象
                return true;
            }
        }
        return false;
    }

    /**
     * 确定构造器
     */
    private void setConstructor() {
        Constructor<?>[] structors = beanModel.getTagClass().getConstructors();
        int count = 10;
        if(structors.length <1) {
            structors = beanModel.getTagClass().getDeclaredConstructors();
            for(Constructor<?> c : structors) {
                if(c.getParameterCount()<count) {
                    this.constructor = c;
                    count = c.getParameterCount();
                }
            }
            constructor.setAccessible(true);
        }else {
            for(Constructor<?> c : structors) {
                if(c.getParameterCount()<count) {
                    this.constructor = c;
                    count = c.getParameterCount();
                }
            }
        }
    }

    public Object[] getArguments() {
    	if(beanModel.getConstructorArgValue()!=null) {
    		int count = beanModel.getConstructorArgValue().getArgumentCount();
    		Object[] objes = new Object[constructor.getParameters().length];
    		if(objes.length!=count) {
    			throw new RuntimeException("异常构建方法");
    		}
    		for(int i=0;i<count;i++) {
    			objes[i] = beanModel.getConstructorArgValue().getArgumentValue(i, getArgumentTypes()[i]).getValue();
    		}
    		return objes;
    	}
        Object[] objes = new Object[constructor.getParameters().length];
        for(int i=0;i<objes.length;i++) {
            Class<?> c = getArgumentTypes()[i];
            if(c == String.class) {
                objes[i] = "";
            }else if(c == Integer.class || c == int.class){
                objes[i] = 0;
            }else if(c == Double.class || c == double.class){
                objes[i] = (double)0;
            }else if(c == Byte.class || c == byte.class){
                objes[i] = (byte)0;
            }else if(c == Long.class || c == long.class){
                objes[i] = 0l;
            }else if(c == Boolean.class || c == boolean.class){
                objes[i] = false;
            }else if(c == Float.class || c == float.class){
                objes[i] = 0.0;
            }else if(c == Short.class || c == short.class ){
                objes[i] = 0;
            }else if(c == char.class){
                objes[i] = '0';
            }else if(c.getName().contains("java.util.List")) {
                objes[i] = Lists.newArrayList();
            }else if(c.getName().contains("java.util.Set")) {
                objes[i] = Sets.newHashSet();
            }
            else {
            	if(getArgumentTypes()[i].getAnnotations().length>0) {
            		objes[i] = LazyBean.getInstance().buildProxy(getArgumentTypes()[i]);
            	}else{
            		if(beanModel.isXmlBean() && beanModel.getConstructorArgs()!=null) {
            			ConstructorArgumentValues args = beanModel.getConstructorArgs();
            			objes[i] = XmlBeanUtil.getInstance().conversionValue(args.getIndexedArgumentValues().get(i).getValue());
            		}else {
            			log.warn("==============未知构造参数==>>{}============",constructor.getParameters()[i].getName());
            		}
            	}
            }
        }
        return objes;
    }
    public Class<?>[] getArgumentTypes() {
        return constructor.getParameterTypes();
    }
    
    @Override
    public Object intercept(Object poxy, Method method, Object[] param, MethodProxy arg3) throws Throwable {
    	return StackOverCheckUtil.observeThrowException(()->{
    		return commonIntercept(poxy, method, param);
    	});
    }
    
    @Override
    protected  synchronized Object getTagertObjectCustom() {
        Class<?> tagertC = beanModel.getTagClass();
//        if(tagertC.getName().contains("JedisCluster")) {
//            log.info("");
//        }
        String beanName = beanModel.getBeanName();
        if(!ScanUtil.exists(tagertC)) {
            if(LazyMongoBean.isMongo(tagertC)) {//，判断是否是Mongo
                tagertObj = LazyMongoBean.buildBean(tagertC,beanName);
            }/*else if(LazyMQBean.isBean(tagertC)) {
//                tagertObj = LazyMQBean.buildBean(tagertC);
                log.info("");
            }*/
            if(tagertObj==null && !inited && !beanModel.isXmlBean()) {
                tagertObj = LazyBean.findCreateBeanFromFactory(tagertC,beanName);
            }
        }
        if (tagertObj == null) {
            ConfigurationProperties propConfig = (ConfigurationProperties) tagertC.getAnnotation(ConfigurationProperties.class);
            if(tagertObj == null){
                if(!LazyBean.existBean(tagertC) && !beanModel.isXmlBean()) {
                    //本地查找是否有构建bean的@Bean方法
                    tagertObj = LazyBean.findCreateBeanFromFactory(tagertC, beanName);
                    if(tagertObj == null) {
                    	if(propConfig == null) {
                    		
                    		Object obj = applicationContext.getBean(beanName);
                    		if(obj==null) {
                    			obj = applicationContext.getBean(tagertC);
                    		}
                    		if(obj!=null && !AbstractLazyProxy.isProxy(obj)) {
                				tagertObj = obj;
                			}
                    		if(tagertObj == null) {
                    			throw new JunitException(tagertC.getName()+" Bean 不存在", true);
                    		}
                    	}
                    	tagertObj = LazyBean.findCreateByProp(tagertC);
                    }
                    
                }
                if(tagertObj == null) {
                    /**
                     * 通过newInstance 创建对象
                     */
                    buildObject();
                }
            }
            
            if(propConfig!=null && tagertObj!=null) {
            	LazyConfPropBind.processConfigurationProperties(tagertObj,propConfig);
            }
            if(tagertObj!=null) {
            	LazyDubboBean.getInstance().processAttr(tagertObj,tagertObj.getClass());
            }
        }
        return tagertObj;
    }
    /**
     * 构建实际对象
     * @param tagertC   实际对象的类
     */
    private void buildObject() {
        Class<?> tagertC = beanModel.getTagClass();
        try {
            if (constructor.getParameterCount() > 0) {
                tagertObj = constructor.newInstance(getArguments());
            } else {
                /**
                 * //直接反射构建目标对象
                 */
                tagertObj = constructor.newInstance();
                LazyBean.getInstance().processAttr(tagertObj, tagertC);// 递归注入代理对象
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException e) {
            log.error("带参构造对象异常", e);
        }
    }

    public boolean findPublicConstrucors() {
         return constructor!=null && Modifier.isPublic(constructor.getModifiers());
    }
}