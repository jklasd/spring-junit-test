package com.github.jklasd.test.lazybean.beanfactory.generics;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.w3c.dom.Element;

import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazyplugn.LazyPlugnBeanFactory;
import com.github.jklasd.test.lazyplugn.dubbo.AbstractRefHandler;
import com.github.jklasd.test.lazyplugn.dubbo.LazyDubboAnnotationRefHandler;
import com.github.jklasd.test.lazyplugn.dubbo.LazyDubboXmlRefHandler;
import com.github.jklasd.test.lazyplugn.spring.BeanDefParser;
import com.github.jklasd.test.lazyplugn.spring.xml.XmlBeanUtil;
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
    public static LazyAbstractPlugnBeanFactory getInstance() {
		return getInstanceByClass(LazyDubboBean.class);
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
    public void load(Map<String, BeanDefParser> parser) {
        XmlBeanUtil.getInstance().getNamespaceURIList().stream().filter(url->url.contains("dubbo")).forEach(url->{
            parser.put(url, this);
        });
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
            return buildBeanNew(dubboClass,model.getBeanName());
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
		return beanNameSets.contains(beanModel.getTagClass().getName());
	}
    
}
