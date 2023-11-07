package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.w3c.dom.Element;

import com.github.jklasd.test.common.interf.handler.BeanDefParser;
import com.github.jklasd.test.common.interf.handler.LazyPlugnBeanFactory;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazyplugn.dubbo.AbstractRefHandler;
import com.github.jklasd.test.lazyplugn.dubbo.LazyDubboAnnotationRefHandler;
import com.github.jklasd.test.lazyplugn.dubbo.LazyDubboXmlRefHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author jubin.zhang
 *
 */
@Slf4j
public class LazyDubboBean extends LazyAbstractPlugnBeanFactory implements BeanDefParser,LazyPlugnBeanFactory{
    public static LazyDubboBean getInstance() {
		return (LazyDubboBean) getInstanceByClass(LazyDubboBean.class);
	}
    
    @Override
	public boolean canBeInstance() {
    	return useDubbo();
	}
    
    @Override
	public String getName() {
		return "LazyDubboBean";
	}
    
	public Integer getOrder() {
		return 200;
	}
	
	@Override
	public boolean isInterfaceBean() {
		return true;
	}
    
	private static Map<Class<?>,Object> dubboData = Maps.newHashMap();
	
	public static final boolean useDubbo() {
		return aliService!=null||apacheService!=null;
	}
	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation> aliService = ScanUtil.loadClass("com.alibaba.dubbo.config.annotation.Service");
	@SuppressWarnings("unchecked")
    private static final Class<? extends Annotation> apacheService = ScanUtil.loadClass("org.apache.dubbo.config.annotation.DubboService");
	public static final Class<? extends Annotation> getAnnotionClass() {
	    if(apacheService!=null) {
	        return apacheService;
	    }
		if(aliService!=null ) {
			return aliService;
		}
		return null;
	}
	private AbstractRefHandler xmlRefHandler = LazyDubboXmlRefHandler.getInstance();
	private AbstractRefHandler annRefHandler = LazyDubboAnnotationRefHandler.getInstance();
	List<String> dubboRefBeanList = Lists.newArrayList();
	
	Set<String> beanNameSets = Sets.newHashSet();
	
	public void handBeanDef(Element ele,BeanDefinition beanDef) {
		String beanName = null;
		Object id = beanDef.getPropertyValues().getPropertyValue("id").getValue();
        switch (ele.getTagName()) {
            case "dubbo:reference":
            	if( id != null ) {
            		dubboRefBeanList.add(id.toString());
//            		LazyListableBeanFactory.getInstance().removeBeanDefinition(id.toString());
            	}
            	beanName = beanDef.getPropertyValues().getPropertyValue("interface").getValue().toString();
            	beanNameSets.add(beanName);
            	xmlRefHandler.getRefType().put(beanName, AbstractRefHandler.TypeXml);
            	xmlRefHandler.getDubboRefferCacheDef().put(beanName, beanDef);
                break;
            case "dubbo:service":
            	beanName = beanDef.getPropertyValues().getPropertyValue("interface").getValue().toString();
            	xmlRefHandler.getDubboServiceCacheDef().put(beanName, beanDef);
                break;
            default:
            	beanName = beanDef.getBeanClassName();
            	xmlRefHandler.getDubboConfigCacheDef().put(beanName, beanDef);
                break;
        }
    }
    public boolean isDubboNew(Class<?> classBean) {
        return xmlRefHandler.isDubboNew(classBean)
        		|| annRefHandler.isDubboNew(classBean);
    }
    
    public Object buildBeanNew(Class<?> dubboClass, String beanName) {
    	if(xmlRefHandler.getRefType().containsKey(dubboClass.getName())) {
    		return xmlRefHandler.buildBeanNew(dubboClass,beanName);
    	}else if(annRefHandler.getRefType().containsKey(dubboClass.getName())) {
    		return annRefHandler.buildBeanNew(dubboClass,beanName);
    	}
    	return null;
    }

    @Override
    public Object buildBean(BeanModel model) {
        if (useDubbo()) {
            Class<?> dubboClass = model.getTagClass();
            String beanName = StringUtils.isBlank(model.getBeanName())?model.getFieldName():model.getBeanName();
            if(StringUtils.isBlank(beanName)) {
            	beanName = dubboClass.getName();
            }
            return buildBeanNew(dubboClass,beanName);
        }
        return null;
    }

//    public void processAttr(Object tagertObj, Class<?> objClassOrSuper) {
//    	LazyDubboAnnotationRefHandler.getInstance().processAttr(tagertObj, objClassOrSuper);
//    }
	public void registerDubboService(Class<?> class1) {
		xmlRefHandler.registerDubboService(class1);
	}
	@Override
	public boolean finded(BeanModel beanModel) {
		String className = beanModel.getTagClass().getName();
		return xmlRefHandler.getRefType().containsKey(className)
				|| annRefHandler.getRefType().containsKey(className);
	}
	
}
