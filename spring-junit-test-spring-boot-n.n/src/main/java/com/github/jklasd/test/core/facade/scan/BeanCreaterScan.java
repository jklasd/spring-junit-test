package com.github.jklasd.test.core.facade.scan;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.ScanUtil;
import com.github.jklasd.test.common.interf.register.BeanFactoryProcessorI;
import com.github.jklasd.test.common.interf.register.BeanScanI;
import com.github.jklasd.test.common.model.AssemblyDTO;
import com.github.jklasd.test.common.util.JunitCountDownLatchUtils;
import com.github.jklasd.test.util.CheckUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanCreaterScan implements BeanScanI{
	private BeanCreaterScan() {}
//	private ApplicationContext coreComponent = TestUtil.getInstance().getApplicationContext();
//	private static Set<String> notFoundSet = Sets.newConcurrentHashSet();
	
	private Object[] findClassMethodByBeanName(AssemblyDTO assemblyData) {
		Object[] address = new Object[2];
		Object[] tmp = new Object[2];
		final String beanName = assemblyData.getBeanName();
		final Class<?> tagC = assemblyData.getTagClass();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(customizeConfigClass))
		.setError((c,e)->{
			log.error(c+"处理异常",e);
		})
		.setException((c,e)->{
			log.error(c+"处理异常",e);
		})
		.runAndWait(c->matchByBeanName(address, tmp, beanName, tagC, c));
		if((address[0] ==null || address[1]==null)
				&& (tmp[0] == null || tmp[1]==null)) {
			
			JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(springConfigClass))
			.setError((c,e)->{
				log.error(c+"处理异常",e);
			})
			.setException((c,e)->{
				log.error(c+"处理异常",e);
			})
			.runAndWait(c->matchByBeanName(address, tmp, beanName, tagC, c));
		}
		if((address[0] ==null || address[1]==null)) {
			return tmp;
		}
		return address;
	}
private void matchByBeanName(Object[] address, Object[] tmp, final String beanName, final Class<?> tagC, Class<?> c) {
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
}
	
	public void checkConfig(String className) {
		Lists.newArrayList(thridAutoConfigClass).forEach(config->{
			if(config.getName().contains(className)) {
				log.info("checked=>>>{}",className);
			}
		});
	}
	
	public Object[] findClassMethodByResultType(AssemblyDTO assemblyData) {
		Object[] tmp = new Object[2];
		final Class<?> tagC = assemblyData.getTagClass();
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(customizeConfigClass))
		.setError((c,e)->{
			log.error(c+"处理异常",e);
		})
		.setException((c,e)->{
			log.error(c+"处理异常",e);
		})
		.runAndWait(c->matchByClass(tmp, tagC, c));
		if(tmp[0] ==null || tmp[1]==null) {
			JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(springConfigClass))
			.setError((c,e)->{
				log.error(c+"处理异常",e);
			})
			.setException((c,e)->{
				log.error(c+"处理异常",e);
			})
			.runAndWait(c->matchByClass(tmp, tagC, c));
		}
		return tmp;
	}
	private void matchByClass(Object[] tmp, final Class<?> tagC, Class<?> c) {
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
	}
	
	public Object[] findCreateBeanFactoryClass(AssemblyDTO assemblyData) {
		if(StringUtils.isNotBlank(assemblyData.getBeanName())) {
			Object[] result = findClassMethodByBeanName(assemblyData);
			if(result[0] != null) {
				return result;
			}
		}
		return findClassMethodByResultType(assemblyData);
	}
	
	private volatile Set<Class<?>> thridAutoConfigClass = Sets.newConcurrentHashSet();
	private volatile Set<Class<?>> customizeConfigClass = Sets.newConcurrentHashSet();
	private volatile Set<Class<?>> springConfigClass = Sets.newConcurrentHashSet();
	public void load(Class<?> configClass) {
		if(!CheckUtil.checkClassExists(configClass)) {
			return;
		}
		log.debug("=============加载{}=============",configClass);
		if(configClass.getName().startsWith(ScanUtil.SPRING_PACKAGE)) {
			springConfigClass.add(configClass);
		}else {
			customizeConfigClass.add(configClass);
		}
		thridAutoConfigClass.add(configClass);
	}
	private static BeanCreaterScan scaner = new BeanCreaterScan();
	public static BeanCreaterScan getInstance() {
		return scaner;
	}
	public boolean contains(Class<?> configClass) {
		return thridAutoConfigClass.contains(configClass);
	}
	public Object[] findCreateBeanFactoryClass(Class<?> tagC) {
		AssemblyDTO assemblyData = new AssemblyDTO();
		assemblyData.setTagClass(tagC);
		return findClassMethodByResultType(assemblyData);
	}

	@Override
	public void register() {
		ContainerManager.registComponent( this);
	}
	
	@Override
	public String getBeanKey() {
		return BeanScanI.class.getSimpleName();
	}

}
