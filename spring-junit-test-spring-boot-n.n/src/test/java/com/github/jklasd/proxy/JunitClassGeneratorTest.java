package com.github.jklasd.proxy;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.CallbackFilter;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.objenesis.ObjenesisStd;

import com.github.jklasd.proxy.ProxyTest.Test1;
import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.lazybean.beanfactory.LazyBean;

public class JunitClassGeneratorTest {
	@Test
	public void test() {
//		Class<?> proxyClass = JunitClassGenerator.getGeneratorInstance(TestMockBean.class).makeClass();
		try {
			Class<?> mockedType = ProxyTest.Test1.class;
	        Enhancer enhancer = new Enhancer() {
	            @Override
	            @SuppressWarnings("unchecked")
	            protected void filterConstructors(Class sc, List constructors) {
	                // Don't filter
	            }
	        };
	        Class<?>[] interfaces = mockedType.getInterfaces();
	        Class<?>[] allMockedTypes = prepend(mockedType, interfaces);
			enhancer.setClassLoader(JunitClassLoader.getInstance());
	        enhancer.setUseFactory(true);
	        if (mockedType.isInterface()) {
	            enhancer.setSuperclass(Object.class);
	            enhancer.setInterfaces(allMockedTypes);
	        } else {
	            enhancer.setSuperclass(mockedType);
	            enhancer.setInterfaces(interfaces);
	        }
	        BeanModel model = new BeanModel();
			model.setTagClass(mockedType);
			Callback callback = LazyBean.createLazyCglib(model);
	        enhancer.setCallbackTypes(new Class[]{callback.getClass()});
	        enhancer.setCallbackFilter(new CallbackFilter() {
				@Override
				public int accept(Method method) {
					return method.isBridge() ? 1 : 0;
				}
			});

	        enhancer.setSerialVersionUID(42L);
	        
			Class<?> proxyClass = enhancer.createClass();
			
			ObjenesisStd objenesis = new ObjenesisStd();
			Factory factory = (Factory) objenesis.newInstance(proxyClass);
			factory.setCallbacks(new Callback[]{callback});
			Test1 test = (Test1) factory;
			test.exec();
		} catch (SecurityException
				| IllegalArgumentException /* | NoSuchMethodException | InvocationTargetException */ e) {
			e.printStackTrace();
		}
	}
	private Class<?>[] prepend(Class<?> first, Class<?>... rest) {
        Class<?>[] all = new Class<?>[rest.length+1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }
}
