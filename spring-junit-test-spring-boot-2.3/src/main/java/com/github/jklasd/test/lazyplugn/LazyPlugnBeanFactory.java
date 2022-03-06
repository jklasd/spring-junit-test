 package com.github.jklasd.test.lazyplugn;

import com.github.jklasd.test.lazybean.beanfactory.AbastractLazyProxy;

public interface LazyPlugnBeanFactory {
     Object buildBean(AbastractLazyProxy model);
}
