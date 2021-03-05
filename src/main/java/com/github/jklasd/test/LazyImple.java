package com.github.jklasd.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.springframework.aop.framework.AopContextSuppert;
import org.springframework.transaction.annotation.Transactional;

import com.github.jklasd.test.db.LazyMongoBean;
import com.github.jklasd.test.db.LazyMybatisMapperBean;
import com.github.jklasd.test.dubbo.LazyDubboBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LazyImple implements InvocationHandler {

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
	private Type[] classGeneric;
	public LazyImple(Class classBean, String beanName2, Type[] classGeneric) {
		this(classBean,beanName2);
		this.classGeneric = classGeneric;
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
		}catch (Exception e) {
			Throwable tmp = e;
			if(e.getCause()!=null) {
				tmp = e.getCause();
			}
			log.error("代理类执行异常=>{},->{}",tag,tmp.getMessage());
			log.error("代理类执行异常",tmp);
			throw new Exception("代理类执行异常");
		}
	}
	/**
	 * 接口类型
	 * 当调用目标对象方法时，对目标对象tagertObj进行实例化
	 * @return
	 */
	private Object getTagertObj() {
//		if(tag.getName().contains("BackstageConfigService")) {
//			log.info("断点");
//		}
		if (tagertObj == null) {
			if(LazyDubboBean.isDubbo(tag)) {//，判断是否是Dubbo服务
				tagertObj = LazyDubboBean.buildBean(tag);
			}else if(LazyMongoBean.isMongo(tag)) {//，判断是否是Mongo
				tagertObj = LazyMongoBean.buildBean(tag,null);
			} else {
				if(LazyMybatisMapperBean.isMybatisBean(tag)) {//判断是否是Mybatis mapper
					isDbConnect = true;
					return LazyMybatisMapperBean.buildBean(tag);//防止线程池执行时，出现获取不到session问题
				}else {
					if(beanName == null) {
						/**
						 * 若是本地接口实现类的bean，则进行bean查找。
						 */
						Object tagImp = LazyBean.findBeanByInterface(tag,classGeneric);
						if(tagImp == null) {
							tagImp = ScanUtil.findCreateBeanFromFactory(tag, beanName);
							if(tagImp == null) {
								log.info("未找到本地Bean=>{}",tag);
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
						Object tagImp = LazyBean.findBean(beanName);
						if(tagImp == null) {
							tagImp = ScanUtil.findCreateBeanFromFactory(tag, beanName);
							if(tagImp == null) {
								log.info("未找到本地Bean=>{}",tag);
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
					}
				}
			}
		}
		return tagertObj;
	}
}
