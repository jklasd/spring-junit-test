package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyDubboBean;
import com.github.jklasd.test.lazybean.beanfactory.generics.LazyMybatisMapperBean;
import com.github.jklasd.test.lazyplugn.db.LazyMongoBean;
import com.github.jklasd.test.util.StackOverCheckUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class LazyImple extends AbstractLazyProxy implements InvocationHandler {

    public LazyImple(BeanModel beanModel) {
        super(beanModel);
        this.classGeneric = beanModel.getClassGeneric();
    }
    
	private Type[] classGeneric;
	@Override
	public Object invoke(Object proxy, Method method, Object[] param) throws Throwable {
		return StackOverCheckUtil.observeThrowException(()->{
			return commonIntercept(proxy, method, param);
    	});
	}
	
    @Override
    protected synchronized Object getTagertObjectCustom() {
        Class<?> tagertC = beanModel.getTagClass();
        String beanName = beanModel.getBeanName();
        
        tagertObj = LazyPlugnBeanFactoryManager.getInstance().getTagertObjectCustom(beanModel);
        if(tagertObj != null) {
        	return tagertObj;
        }
        
        if(LazyDubboBean.getInstance().isDubboNew(tagertC)) {//，判断是否是Dubbo服务
            tagertObj = LazyDubboBean.getInstance().buildBeanNew(tagertC,beanName);
        }else if(LazyMongoBean.isMongo(tagertC)) {//，判断是否是Mongo
            tagertObj = LazyMongoBean.buildBean(tagertC,beanName);
        } else {
            if(LazyMybatisMapperBean.getInstance().finded(beanModel)) {//判断是否是Mybatis mapper
                return LazyMybatisMapperBean.getInstance().buildBean(beanModel);//防止线程池执行时，出现获取不到session问题
            }else {
                if(beanName == null) {
                    /**
                     * 若是本地接口实现类的bean，则进行bean查找。
                     */
                    Object tagImp = LazyBean.getInstance().findBeanByInterface(tagertC,classGeneric);
                    if(tagImp == null) {
                        tagImp = LazyBean.findCreateBeanFromFactory(tagertC, beanName);
                        if(tagImp == null) {
                            log.info("未找到本地Bean=>{}",tagertC);
//                            tagertObj = TestUtil.getApplicationContext().getBeanByClass(tagertC);
                        }else {
                            tagertObj = tagImp;
                        }
                    }else {
                        /**
                         * 实现类是本地Bean
                         */
                        tagertObj = tagImp;
                        LazyBean.getInstance().processAttr(tagImp, tagImp.getClass());
                    }
                }else {
                    // 本地bean
                    Object tagImp = LazyBean.findCreateBeanFromFactory(tagertC, beanName);
                    if(tagImp == null) {
                        tagImp = LazyBean.getInstance().createBeanForProxy(beanName, tagertC);
                        if(tagImp == null) {
                            log.info("未找到本地Bean=>{}",tagertC);
                        }
                    }
                    if(tagImp != null) {
                        LazyBean.getInstance().processAttr(tagImp, tagImp.getClass());
                        tagertObj = tagImp;
                    }
                }
//                if(tagertObj!=null) {
//                    LazyDubboBean.getInstance().processAttr(tagertObj,tagertObj.getClass());
//                }
            }
        }
        return tagertObj;
    }
}
