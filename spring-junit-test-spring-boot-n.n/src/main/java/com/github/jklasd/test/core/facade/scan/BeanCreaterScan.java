package com.github.jklasd.test.core.facade.scan;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

import com.github.jklasd.test.common.abstrac.JunitListableBeanFactory;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.common.interf.register.BeanScanI;
import com.github.jklasd.test.common.model.BeanModel;
import com.github.jklasd.test.common.model.JunitMethodDefinition;
import com.github.jklasd.test.common.util.CheckUtil;
import com.github.jklasd.test.common.util.JunitCountDownLatchUtils;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.lazyplugn.spring.JavaBeanUtil;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanCreaterScan implements BeanScanI{
	private BeanCreaterScan() {}
	private JunitListableBeanFactory beanFactory = LazyListableBeanFactory.getInstance();
	
	private JunitMethodDefinition findClassMethodByBeanName(BeanModel assemblyData) {
		return beanJmdMap.get(assemblyData.getBeanName());
	}
	
	
	public void checkConfig(String className) {
		Lists.newArrayList(thridAutoConfigClass).forEach(config->{
			if(config.getName().contains(className)) {
				log.info("checked=>>>{}",className);
			}
		});
	}
	
	public JunitMethodDefinition findClassMethodByResultType(BeanModel assemblyData) {
		final Class<?> tagC = assemblyData.getTagClass();
		
		JunitMethodDefinition tmp = matchByClass(tagC,customizeJmds);
		if(tmp != null) {
			return tmp;
		}
		tmp = matchByClass(tagC,springBootJmds);
		if(tmp != null) {
			return tmp;
		}
		return null;
	}

	private JunitMethodDefinition matchByClass(final Class<?> tagC, List<JunitMethodDefinition> jmds) {
		for(JunitMethodDefinition jmd : jmds) {
			if(jmd == null || jmd.getReturnType() == null) {
				log.warn("查看异常问题");
			}
//			if(tagC.getName().contains("ReactorServiceInstanceLoadBalancer")
//					&& jmd.getConfigurationClass().getName().contains("LoadBalancerClientConfiguration")) {
//				log.debug("查看问题");
//			}
			if(ScanUtil.isExtends(jmd.getReturnType(), tagC) || ScanUtil.isImple(jmd.getReturnType(), tagC) || jmd.getReturnType() == tagC) {
				return jmd;
			}
			
			if(ScanUtil.isExtends(tagC, jmd.getReturnType())) {
				BeanModel assemblyData = new BeanModel();
				assemblyData.setTagClass(tagC);
				Object tagObj = JavaBeanUtil.getInstance().buildBean(jmd.getConfigurationClass(),jmd.getMethod(),assemblyData);
				if(tagC.isAssignableFrom(tagObj.getClass())) {
					return jmd;
				}
			}
			
			if(ScanUtil.isImple(jmd.getReturnType(), FactoryBean.class)) {
				Class<?> resultType =  jmd.getReturnType();
				try {
					if(resultType.getMethod("getObject").getReturnType() == tagC) {
						return jmd;
					}
				} catch (NoSuchMethodException | SecurityException e) {
					throw new JunitException(e);
				}
			}
		}
		return null;
	}
	
	@Override
	public List<JunitMethodDefinition> findCreateBeanFactoryClasses(BeanModel assemblyData) {
		registMethodBeanDefinition();
		return findClassMethodsByResultType(assemblyData);
	}
	
	private List<JunitMethodDefinition> findClassMethodsByResultType(BeanModel assemblyData) {
		final Class<?> tagC = assemblyData.getTagClass();
		List<JunitMethodDefinition> list = Lists.newArrayList();
		list.addAll(matchByClasses(tagC,customizeJmds));
		list.addAll(matchByClasses(tagC,springBootJmds));
		return list;
	}


	private List<JunitMethodDefinition> matchByClasses(Class<?> tagC, List<JunitMethodDefinition> jmds) {
		List<JunitMethodDefinition> list = Lists.newArrayList();
		for(JunitMethodDefinition jmd : jmds) {
			if(jmd == null || jmd.getReturnType() == null) {
				log.warn("查看异常问题");
			}
//			if(tagC.getName().contains("ReactorServiceInstanceLoadBalancer")
//					&& jmd.getConfigurationClass().getName().contains("LoadBalancerClientConfiguration")) {
//				log.debug("查看问题");
//			}
			if(ScanUtil.isExtends(jmd.getReturnType(), tagC) || ScanUtil.isImple(jmd.getReturnType(), tagC) || jmd.getReturnType() == tagC) {
				list.add(jmd);
				continue;
			}
			
			if(ScanUtil.isExtends(tagC, jmd.getReturnType())) {
				BeanModel assemblyData = new BeanModel();
				assemblyData.setTagClass(tagC);
				Object tagObj = JavaBeanUtil.getInstance().buildBean(jmd.getConfigurationClass(),jmd.getMethod(),assemblyData);
				if(tagC.isAssignableFrom(tagObj.getClass())) {
					list.add(jmd);
					continue;
				}
			}
			
			if(ScanUtil.isImple(jmd.getReturnType(), FactoryBean.class)) {
				Class<?> resultType =  jmd.getReturnType();
				try {
					if(resultType.getMethod("getObject").getReturnType() == tagC) {
						list.add(jmd);
						continue;
					}
				} catch (NoSuchMethodException | SecurityException e) {
					throw new JunitException(e);
				}
			}
		}
		return list;
	}


	public JunitMethodDefinition findCreateBeanFactoryClass(BeanModel assemblyData) {
		registMethodBeanDefinition();
		if(StringUtils.isNotBlank(assemblyData.getBeanName())) {
			JunitMethodDefinition result = findClassMethodByBeanName(assemblyData);
			if(result!=null) {
				return result;
			}
			//通过beanname 没找到
		}
		return findClassMethodByResultType(assemblyData);
	}
	
	private volatile Set<Class<?>> thridAutoConfigClass = Sets.newConcurrentHashSet();
	private volatile Set<Class<?>> customizeConfigClass = Sets.newConcurrentHashSet();
	private volatile Set<Class<?>> springConfigClass = Sets.newConcurrentHashSet();
	public void loadConfigurationClass(Class<?> configClass) {
		if(!CheckUtil.checkClassExists(configClass)) {
			log.info("=============不能加载{}=============",configClass);
			return;
		}
//		if(configClass.getName().contains("LoadBalancerClientConfiguration")) {
//			log.debug("=============加载{}=============",configClass);
//		}
		if(configClass.getName().startsWith(ScanUtil.SPRING_PACKAGE)) {
			springConfigClass.add(configClass);
			if(init>1) {
				handSpringJmd(configClass);
				log.info("after add springBootJmds#size:{}",springBootJmds.size());
			}
		}else {
			customizeConfigClass.add(configClass);
			if(init>1) {
				handlCustomizedJmd(configClass);
				log.info("after add customizeJmds#size:{}",customizeJmds.size());
			}
		}
		thridAutoConfigClass.add(configClass);
	}
	private static BeanCreaterScan scaner = new BeanCreaterScan();
	public static BeanCreaterScan getInstance() {
		return scaner;
	}
//	public boolean contains(Class<?> configClass) {
//		return thridAutoConfigClass.contains(configClass);
//	}
	public JunitMethodDefinition findCreateBeanFactoryClass(Class<?> tagC) {
		registMethodBeanDefinition();
		BeanModel assemblyData = new BeanModel();
		assemblyData.setTagClass(tagC);
		return findClassMethodByResultType(assemblyData);
	}

	@Override
	public String getBeanKey() {
		return BeanScanI.class.getSimpleName();
	}

	/**
	 * 注册所有关于@Bean Definition
	 */
	
	private Map<String,JunitMethodDefinition> beanJmdMap = Maps.newConcurrentMap();
	private List<JunitMethodDefinition> springBootJmds = Lists.newCopyOnWriteArrayList();
	private List<JunitMethodDefinition> customizeJmds = Lists.newCopyOnWriteArrayList();
	
	volatile int init;
	public synchronized void registMethodBeanDefinition() {
		if(init>0) {
			if(init<2) {
				log.info("还在准备中……");
			}
			return;
		}
		init = 1;
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(customizeConfigClass))
		.setError((c,e)->log.error(c+"处理异常",e))
		.setException((c,e)->log.error(c+"处理异常",e))
		.runAndWait(configClass->{
			handlCustomizedJmd(configClass);
		});
		log.info("customizeJmds#size:{}",customizeJmds.size());
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(springConfigClass))
		.setError((c,e)->log.error(c+"处理异常",e))
		.setException((c,e)->log.error(c+"处理异常",e))
		.runAndWait(configClass->{
			handSpringJmd(configClass);
		});
		
		log.info("springBootJmds#size:{}",springBootJmds.size());
		init = 2;
	}


	private void handSpringJmd(Class<?> configClass) {
		Method[] methods = configClass.getDeclaredMethods();
		for(Method m : methods) {
			
			if(AnnotatedElementUtils.hasAnnotation(m, Bean.class)) {
				
				String beanName = determineBeanNameFor(m);
				
				JunitMethodDefinition jmd = new JunitMethodDefinition();
				jmd.setBeanName(beanName);
				jmd.setMethod(m);
				jmd.setReturnType(m.getReturnType());
				jmd.setConfigurationClass(configClass);
			
				if(!CheckUtil.checkClassExistsForMethod(m)) {
//					log.debug("排除：{}",m.getName());
					continue;
				}
//				System.out.println("m:"+jmd.getMethod()+"r:"+jmd.getReturnType());
				springBootJmds.add(jmd);
				
				beanJmdMap.putIfAbsent(beanName, jmd);
			}
		}
	}


	private void handlCustomizedJmd(Class<?> configClass) {
		Method[] methods = configClass.getDeclaredMethods();
		for(Method m : methods) {
			
			if(AnnotatedElementUtils.hasAnnotation(m, Bean.class)) {
				
				String beanName = determineBeanNameFor(m);
				
				JunitMethodDefinition jmd = new JunitMethodDefinition();
				jmd.setBeanName(beanName);
				jmd.setMethod(m);
				jmd.setReturnType(m.getReturnType());
				jmd.setConfigurationClass(configClass);
				
				//加强校验
				if(!CheckUtil.checkClassExistsForMethod(m)) {
//					log.debug("排除：{}",m.getName());
					continue;
				}
				customizeJmds.add(jmd);
				
				beanJmdMap.put(beanName, jmd);
			}
		}
	}

	private String determineBeanNameFor(Method beanMethod) {
		String beanName = beanMethod.getName();
		AnnotationAttributes bean =
				AnnotatedElementUtils.findMergedAnnotationAttributes(beanMethod, Bean.class, false, false);
		if (bean != null) {
			String[] names = bean.getStringArray("name");
			if (names.length > 0) {
				beanName = names[0];
			}
		}
		return beanName;
	}


	@Override
	public boolean isConfigurationClass(Class<?> configClass) {
		return customizeConfigClass.contains(configClass)
				|| springConfigClass.contains(configClass);
	}
}
