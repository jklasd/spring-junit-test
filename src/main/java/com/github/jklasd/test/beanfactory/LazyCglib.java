package com.github.jklasd.test.beanfactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.db.LazyMongoBean;
import com.github.jklasd.test.mq.LazyMQBean;
import com.github.jklasd.test.spring.LazyConfigurationPropertiesBindingPostProcessor;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyCglib extends LazyProxy implements MethodInterceptor {
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
                objes[i] = LazyBean.getInstance().buildProxy(getArgumentTypes()[i]);
            }
        }
        return objes;
    }
    public Class<?>[] getArgumentTypes() {
        return constructor.getParameterTypes();
    }
    
    @Override
    public Object intercept(Object poxy, Method method, Object[] param, MethodProxy arg3) throws Throwable {
        return commonIntercept(poxy, method, param);
    }
    
    @Override
    protected Object getTagertObjectCustom() {
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
                    if(propConfig==null     || !ScanUtil.findCreateBeanForConfigurationProperties(tagertC)) {
                        throw new RuntimeException(tagertC.getName()+" Bean 不存在");
                    }
                }
                
                if(constructor.getParameterCount()>0) {
                    try {
                        tagertObj = constructor.newInstance(getArguments());
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException e) {
                        log.error("带参构造对象异常",e);
                    }
                }else {
                    /**
                     * 待优化
                     */
                    try {//直接反射构建目标对象
                        tagertObj = tagertC.newInstance();
                        LazyBean.getInstance().processAttr(tagertObj, tagertC);//递归注入代理对象
                    } catch (InstantiationException | IllegalAccessException e) {
                        log.error("构建bean=>{}",tagertC);
                        log.error("构建bean异常",e);
                    }
                }
            }
            if(propConfig!=null && tagertObj!=null) {
                LazyConfigurationPropertiesBindingPostProcessor.processConfigurationProperties(tagertObj,propConfig);
            }
        }
         return tagertObj;
    }
}