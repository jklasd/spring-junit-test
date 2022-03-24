 package com.github.jklasd.test.lazyplugn;

import com.github.jklasd.test.lazybean.beanfactory.AbstractLazyProxy;

public interface LazyPlugnBeanFactory {
     Object buildBean(AbstractLazyProxy model);
}
