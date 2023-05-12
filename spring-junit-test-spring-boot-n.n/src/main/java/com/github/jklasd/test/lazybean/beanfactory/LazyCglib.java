package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import com.github.jklasd.test.common.component.LazyPlugnBeanFactoryComponent;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.util.StackOverCheckUtil;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class LazyCglib extends AbstractLazyProxy implements MethodInterceptor {
    @Getter
    private Constructor<?> constructor;
    public LazyCglib(BeanModel beanModel) {
        super(beanModel);
    }
    
    @Override
    public Object intercept(Object poxy, Method method, Object[] param, MethodProxy arg3) throws Throwable {
    	return StackOverCheckUtil.observeThrowException(()->{
    		return commonIntercept(poxy, method, param);
    	});
    }
    
    @Override
    protected  synchronized Object getTagertObjectCustom() {
    	
    	tagertObj = LazyPlugnBeanFactoryComponent.getInstance().getTagertObjectCustomForClass(beanModel);
    	if(tagertObj == null) {
    		log.warn("{},未找到bean",beanModel);
    	}
    	return tagertObj;
    }
}