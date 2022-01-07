package com.github.jklasd.test.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

public class BeanNameUtil {

	public synchronized static String getBeanName(Class<?> classBean) {
		Component comp = (Component) classBean.getAnnotation(Component.class);
		if(comp!=null && StringUtils.isNotBlank(comp.value())) {
			return comp.value();
		}
		Service service = (Service) classBean.getAnnotation(Service.class);
		if(service!=null && StringUtils.isNotBlank(service.value())) {
			return service.value();
		}
		if(classBean.isInterface()) {
		    return null;
		}
		if(classBean.getSimpleName().length()<1) {
//		    log.info("name=>{}",classBean.getSimpleName());
		    return null;
		}
		return classBean.getSimpleName().substring(0,1).toLowerCase()+classBean.getSimpleName().substring(1);
	}
	public synchronized static String getBeanNameFormAnno(Class<?> classBean) {
        Component comp = (Component) classBean.getAnnotation(Component.class);
        if(comp!=null && StringUtils.isNotBlank(comp.value())) {
            return comp.value();
        }
        Service service = (Service) classBean.getAnnotation(Service.class);
        if(service!=null && StringUtils.isNotBlank(service.value())) {
            return service.value();
        }
        return null;
    }
}
