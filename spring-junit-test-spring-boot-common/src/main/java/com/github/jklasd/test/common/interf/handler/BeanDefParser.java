 package com.github.jklasd.test.common.interf.handler;

import org.springframework.beans.factory.config.BeanDefinition;
import org.w3c.dom.Element;

public interface BeanDefParser {
     public void handBeanDef(Element ele ,BeanDefinition beanDef);
}
