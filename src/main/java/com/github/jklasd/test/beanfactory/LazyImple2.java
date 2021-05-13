package com.github.jklasd.test.beanfactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.db.LazyMongoBean;
import com.github.jklasd.test.db.LazyMybatisMapperBean;
import com.github.jklasd.test.dubbo.LazyDubboBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyImple2 extends LazyProxy implements InvocationHandler {

    public LazyImple2(BeanModel beanModel) {
        super(beanModel);
        this.classGeneric = beanModel.getClassGeneric();
    }
    
	private Type[] classGeneric;
	@Override
	public Object invoke(Object proxy, Method method, Object[] param) throws Throwable {
	    return commonIntercept(proxy, method, param);
	}
	
    @Override
    protected Object getTagertObjectCustom() {
        Class<?> tagertC = beanModel.getTagClass();
        String beanName = beanModel.getBeanName();
        if(LazyDubboBean.getInstance().isDubboNew(tagertC)) {//，判断是否是Dubbo服务
            tagertObj = LazyDubboBean.getInstance().buildBeanNew(tagertC);
        }else if(LazyMongoBean.isMongo(tagertC)) {//，判断是否是Mongo
            tagertObj = LazyMongoBean.buildBean(tagertC,beanName);
        } else {
            if(LazyMybatisMapperBean.isMybatisBean(tagertC)) {//判断是否是Mybatis mapper
                return LazyMybatisMapperBean.getInstance().buildBean(tagertC);//防止线程池执行时，出现获取不到session问题
            }else {
                if(beanName == null) {
                    /**
                     * 若是本地接口实现类的bean，则进行bean查找。
                     */
                    Object tagImp = LazyBean.findBeanByInterface(tagertC,classGeneric);
                    if(tagImp == null) {
                        tagImp = LazyBean.findCreateBeanFromFactory(tagertC, beanName);
                        if(tagImp == null) {
                            log.info("未找到本地Bean=>{}",tagertC);
                        }else {
                            tagertObj = tagImp;
                        }
                    }else {
                        /**
                         * 实现类是本地Bean
                         */
                        tagertObj = tagImp;
                        LazyBean.processAttr(tagImp, tagImp.getClass());
                    }
                }else {
                    // 本地bean
                    Object tagImp = LazyBean.findCreateBeanFromFactory(tagertC, beanName);
                    if(tagImp == null) {
                        tagImp = LazyBean.createBeanForProxy(beanName, tagertC);
                        if(tagImp == null) {
                            log.info("未找到本地Bean=>{}",tagertC);
                        }
                    }
                    if(tagImp != null) {
                        LazyBean.processAttr(tagImp, tagImp.getClass());
                        tagertObj = tagImp;
                    }
                }
            }
        }
        return tagertObj;
    }
}
