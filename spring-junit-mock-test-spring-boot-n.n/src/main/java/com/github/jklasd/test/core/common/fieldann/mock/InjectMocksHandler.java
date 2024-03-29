package com.github.jklasd.test.core.common.fieldann.mock;

import static org.mockito.Mockito.withSettings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;

import com.github.jklasd.test.common.component.FieldAnnComponent;
import com.github.jklasd.test.common.interf.handler.FieldHandler;
import com.github.jklasd.test.common.model.BeanInitModel;
import com.github.jklasd.test.common.model.FieldDef;
import com.github.jklasd.test.core.common.fieldann.AbstractMockHandler;
import com.github.jklasd.test.core.common.fieldann.MockFieldHandler;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

/**
 * 需要后置处理
 * @author jubin.zhang
 *
 */
public class InjectMocksHandler extends AbstractMockHandler implements FieldHandler{

	@Override
	public String getType() {
		return InjectMocks.class.getName();
	}
	
	@Override
	public int order() {//比Autowrite 大1
		return 1;
	}

	
	
	@Override
	public void handler(FieldDef def, Annotation ann) {
		Field attr = def.getField();
		Object tagObject = def.getTagObj();
//		InjectMocks mockAnn = (InjectMocks) ann;
		
		try {
			String bName = attr.getAnnotation(Qualifier.class)!=null?attr.getAnnotation(Qualifier.class).value():null;
			//如果存在spring 注解
			Object obj = null;
			if(!attr.isAccessible()) {
				attr.setAccessible(true);
			}
			Constructor<?>[] constrs = attr.getType().getDeclaredConstructors();
			boolean noParam = false;
			for(Constructor<?> c : constrs) {
				if(c.getParameterCount()<1) {
					noParam = true;
					break;
				}
			}
			if(noParam) {
				//创建mockbean 注册到容器中，需要兼容spring 注入注解
				obj = Mockito.mock(attr.getType(), withSettings()
						.defaultAnswer(Mockito.CALLS_REAL_METHODS)
						.name(attr.getName()));
			}else {
				if(constrs.length == 1) {
					Class<?>[] params = constrs[0].getParameterTypes();
					Object[] objs = new Object[params.length];
					
					for(int i=0;i<params.length;i++) {
						objs[i] = LazyBean.getInstance().buildProxy(params[i]);
					}
					
					obj = Mockito.mock(attr.getType(), withSettings()
							.useConstructor(objs)
							.defaultAnswer(Mockito.CALLS_REAL_METHODS)
							.name(attr.getName()));
				}else {
					obj = Mockito.mock(attr.getType(), withSettings()
							.defaultAnswer(Mockito.CALLS_REAL_METHODS)
							.name(attr.getName()));
				}
			}
			//则注入，在类结束后释放掉对象
			if(attr.get(tagObject)==null) {
				//针对mockbean处理
				FieldAnnComponent.setObj(attr, tagObject, obj);
			}else {//已被spring注入
				//填充
				BeanInitModel model = new BeanInitModel();
				model.setObj(obj);
				model.setTagClass(attr.getType());
				model.setBeanName(bName);
				LazyBean.getInstance().processAttr(model);
				//mockHandClass 重新填充
				
			}
			MockFieldHandler.getInstance().load(tagObject.getClass(),obj,attr.getType(),bName);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
	}
}
