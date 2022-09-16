 package com.github.jklasd.test.lazyplugn;

import com.github.jklasd.test.lazybean.beanfactory.BaseAbstractLazyProxy;

public interface LazyPlugnBeanFactory {
     Object buildBean(BaseAbstractLazyProxy model);
}
