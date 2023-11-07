package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.github.jklasd.test.common.component.LazyPlugnBeanFactoryComponent;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.util.StackOverCheckUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class LazyImple extends AbstractLazyProxy implements InvocationHandler {

    public LazyImple(BeanModel beanModel) {
        super(beanModel);
//        this.classGeneric = beanModel.getClassGeneric();
    }
    
//	private Type[] classGeneric;
	@Override
	public Object invoke(Object proxy, Method method, Object[] param) throws Throwable {
		return StackOverCheckUtil.observeThrowException(()->{
			return commonIntercept(proxy, method, param);
    	});
	}
	
    @Override
    protected synchronized Object getTagertObjectCustom() {
        tagertObj = LazyPlugnBeanFactoryComponent.getInstance().getTagertObjectCustomForInterface(beanModel);
        return tagertObj;
    }
}
