package com.github.jklasd.test.lazybean.beanfactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.github.jklasd.test.common.model.BeanInitModel;
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
        
        tagertObj = LazyPlugnBeanFactoryManager.getInstance().getTagertObjectCustomForInterface(beanModel);
        if(tagertObj != null) {
        	return tagertObj;
        }
        
//        Class<?> tagertC = beanModel.getTagClass();
//        String beanName = beanModel.getBeanName();
//        if(beanName == null) {
//            /**
//             * 若是本地接口实现类的bean，则进行bean查找。
//             */
//            Object tagImp = LazyBean.getInstance().findBeanByInterface(tagertC,classGeneric);
//            if(tagImp == null) {
//                tagImp = LazyBean.findCreateBeanFromFactory(tagertC, beanName);
//                if(tagImp == null) {
//                    log.info("未找到本地Bean=>{}",tagertC);
////                    tagertObj = TestUtil.getApplicationContext().getBeanByClass(tagertC);
//                }else {
//                    tagertObj = tagImp;
//                }
//            }else {
//                /**
//                 * 实现类是本地Bean
//                 */
//                tagertObj = tagImp;
////                LazyBean.getInstance().processAttr(tagImp, tagImp.getClass());
//                BeanInitModel model = new BeanInitModel();
//        		model.setObj(tagertObj);
//        		model.setTagClass(tagImp.getClass());
//        		model.setBeanName(tagImp.getClass().getName());
//        		LazyBean.getInstance().processAttr(model);// 递归注入代理对象
//            }
//        }else {
//            // 本地bean
//            Object tagImp = LazyBean.findCreateBeanFromFactory(tagertC, beanName);
//            if(tagImp == null) {
//                tagImp = LazyBean.getInstance().createBeanForProxy(beanName, tagertC);
//                if(tagImp == null) {
//                    log.info("未找到本地Bean=>{}",tagertC);
//                }
//            }
//            if(tagImp != null) {
////                LazyBean.getInstance().processAttr(tagImp, tagImp.getClass());
//                BeanInitModel model = new BeanInitModel();
//        		model.setObj(tagImp);
//        		model.setTagClass(tagImp.getClass());
//        		model.setBeanName(beanName);
//        		LazyBean.getInstance().processAttr(model);// 递归注入代理对象
//                tagertObj = tagImp;
//            }
//        }
        return tagertObj;
    }
}
