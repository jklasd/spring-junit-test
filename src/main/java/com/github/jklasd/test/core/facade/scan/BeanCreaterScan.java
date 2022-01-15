package com.github.jklasd.test.core.facade.scan;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.core.facade.Scan;
import com.github.jklasd.test.lazybean.model.AssemblyDTO;
import com.github.jklasd.test.util.CheckUtil;
import com.github.jklasd.test.util.DebugObjectView;
import com.github.jklasd.test.util.JunitCountDownLatchUtils;
import com.github.jklasd.test.util.ScanUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanCreaterScan implements Scan{
	private BeanCreaterScan() {}
	@Override
	public void scan() {
		
	}
//	private ApplicationContext coreComponent = TestUtil.getInstance().getApplicationContext();
//	private static Set<String> notFoundSet = Sets.newConcurrentHashSet();
	
	private Object[] findClassMethodByBeanName(AssemblyDTO assemblyData) {
		Object[] address = new Object[2];
		Object[] tmp = new Object[2];
		final String beanName = assemblyData.getBeanName();
		final Class<?> tagC = assemblyData.getTagClass();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(thridAutoConfigClass))
		.setError((c,e)->{
			log.error(c+"处理异常",e);
		})
		.setException((c,e)->{
			log.error(c+"处理异常",e);
		})
		.runAndWait(c->{
			Method[] methods = c.getDeclaredMethods();
			for(Method m : methods) {
				Bean beanA = m.getAnnotation(Bean.class);
				if(beanA != null) {
				    String[] beanNames = beanA.value();
			        for(String bn : beanNames) {
			            if(Objects.equals(bn, beanName)) {
			                address[0]=c;
			                address[1]=m;
			                break;
			            }
			        }
			        if(m.getName().equals(beanName) && tagC!=null) {
			        	if(ScanUtil.isExtends(m.getReturnType(), tagC) || ScanUtil.isImple(m.getReturnType(), tagC) || m.getReturnType() == tagC) {
							tmp[0] = c;
							tmp[1] = m;
							break;
						}
			        }
				}
			}
		});
		if(address[0] ==null || address[1]==null) {
			return tmp;
		}
		return address;
	}
	public Object[] findClassMethodByResultType(AssemblyDTO assemblyData) {
		Object[] address = new Object[2];
		Object[] tmp = new Object[2];
		final Class<?> tagC = assemblyData.getTagClass();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(thridAutoConfigClass))
		.setError((c,e)->{
			log.error(c+"处理异常",e);
		})
		.setException((c,e)->{
			log.error(c+"处理异常",e);
		})
		.runAndWait(c->{
			Method[] methods = c.getDeclaredMethods();
			for(Method m : methods) {
				Bean beanA = m.getAnnotation(Bean.class);
				if(beanA != null) {
					if(tagC!=null) {
						if(!tagC.isInterface() && m.getReturnType().isInterface()) {
							break;
						}
						if(ScanUtil.isExtends(m.getReturnType(), tagC) || ScanUtil.isImple(m.getReturnType(), tagC) || m.getReturnType() == tagC) {
							tmp[0] = c;
							tmp[1] = m;
							break;
						}
					}
				}
			}
		});
		if(address[0] ==null || address[1]==null) {
			return tmp;
		}
		return address;
	}
	
	public Object[] findCreateBeanFactoryClass(AssemblyDTO assemblyData) {
		if(StringUtils.isNoneBlank(assemblyData.getBeanName())) {
			Object[] result = findClassMethodByBeanName(assemblyData);
			if(result[0] != null) {
				return result;
			}
		}
		return findClassMethodByResultType(assemblyData);
	}
	
	private volatile Set<Class<?>> thridAutoConfigClass = Sets.newConcurrentHashSet();
	public void load(Class<?> configClass) {
		if(!CheckUtil.checkClassExists(configClass)) {
			return;
		}
		log.debug("=============加载{}=============",configClass);
		thridAutoConfigClass.add(configClass);
	}
	private static BeanCreaterScan scaner = new BeanCreaterScan();
	public static BeanCreaterScan getInstance() {
		return scaner;
	}
	public boolean contains(Class<?> configClass) {
		return thridAutoConfigClass.contains(configClass);
	}

}
