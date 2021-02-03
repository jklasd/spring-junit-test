package com.junit.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.springframework.aop.framework.AopContextSuppert;
import org.springframework.transaction.annotation.Transactional;

import com.junit.test.db.LazyMongoBean;
import com.junit.test.db.LazyMybatisMapperBean;
import com.junit.test.dubbo.LazyDubboBean;

import lombok.extern.slf4j.Slf4j;

public @Slf4j
class LazyImple implements InvocationHandler {

	private Class tag;
	private Object tagertObj;
	private String beanName;
	private boolean isDbConnect;

	public LazyImple(Class tag) {
		this.tag = tag;
	}
	public LazyImple(Class tag,String beanName) {
		this.tag = tag;
		this.beanName = beanName;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			AopContextSuppert.setProxyObj(proxy);
			Object result = method.invoke(getTagertObj(), args);
			if(!isDbConnect) {
				// 处理openSession
				Transactional transactional = method.getAnnotation(Transactional.class);
			}
			LazyMybatisMapperBean.over();
			return result;
		} catch (Exception e) {
			log.error("代理类执行异常=>{}",tag);
			log.error("代理类执行异常",e);
			//throw e;
		}
		return null;
	}
	/**
	 * 接口类型
	 * 当调用目标对象方法时，对目标对象tagertObj进行实例化
	 * @return
	 */
	private Object getTagertObj() {
		if (tagertObj == null) {
			if(LazyDubboBean.isDubbo(tag)) {//，判断是否是Dubbo服务
				tagertObj = LazyDubboBean.buildBean(tag);
			}else if(LazyMongoBean.isMongo(tag)) {//，判断是否是Mongo
				tagertObj = LazyMongoBean.buildBean(tag,null);
			} else {
				if(LazyMybatisMapperBean.isMybatisBean(tag)) {//判断是否是Mybatis mapper
					//延迟处理
//					tagertObj = LazyMybatisMapperBean.buildBean(tag);
					isDbConnect = true;
					return LazyMybatisMapperBean.buildBean(tag);//防止线程池执行时，出现获取不到session问题
				}else {
					if(beanName == null) {
						/**
						 * 若是本地接口实现类的bean，则进行bean查找。
						 */
						Object tagImp = ScanUtil.findBeanByInterface(tag);
						if(tagImp == null) {
							log.info("未找到本地Bean=>{}",tag);
						}else {
							/**
							 * 实现类是本地Bean
							 */
							tagertObj = tagImp;
							LazyBean.processAttr(tagImp, tagImp.getClass());
						}
					}else {
						// 本地bean
						Object tagImp = ScanUtil.findBean(beanName);
						if(tagImp == null) {
							log.info("未找到本地Bean=>{}",tag);
						}else {
							/**
							 * 实现类是本地Bean
							 */
							tagertObj = tagImp;
							LazyBean.processAttr(tagImp, tagImp.getClass());
						}
					}
				}
			}
		}
		return tagertObj;
	}
}
