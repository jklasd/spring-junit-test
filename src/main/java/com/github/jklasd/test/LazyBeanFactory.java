 package com.github.jklasd.test;

import com.github.jklasd.test.beanfactory.LazyProxy;

public interface LazyBeanFactory {
     Object buildBean(LazyProxy model);
}
