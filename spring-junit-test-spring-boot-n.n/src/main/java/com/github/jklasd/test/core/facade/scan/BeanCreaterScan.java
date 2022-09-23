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

import com.github.jklasd.test.common.ContainerManager;
import com.github.jklasd.test.common.abstrac.JunitListableBeanFactory;
import com.github.jklasd.test.common.interf.register.BeanScanI;
import com.github.jklasd.test.common.model.AssemblyDTO;
import com.github.jklasd.test.common.model.JunitMethodDefinition;
import com.github.jklasd.test.common.util.CheckUtil;
import com.github.jklasd.test.common.util.JunitCountDownLatchUtils;
import com.github.jklasd.test.common.util.ScanUtil;
import com.github.jklasd.test.exception.JunitException;
import com.github.jklasd.test.lazyplugn.spring.LazyListableBeanFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanCreaterScan implements BeanScanI{
	private BeanCreaterScan() {}
	private JunitListableBeanFactory beanFactory = LazyListableBeanFactory.getInstance();
//	private static Set<String> notFoundSet = Sets.newConcurrentHashSet();
	
	private JunitMethodDefinition findClassMethodByBeanName(AssemblyDTO assemblyData) {
		return beanJmdMap.get(assemblyData.getBeanName());
//		Object[] address = new Object[2];
//		Object[] tmp = new Object[2];
//		final String beanName = assemblyData.getBeanName();
//		final Class<?> tagC = assemblyData.getTagClass();
//		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(customizeConfigClass))
//		.setError((c,e)->{
//			log.error(c+"处理异常",e);
//		})
//		.setException((c,e)->{
//			log.error(c+"处理异常",e);
//		})
//		.runAndWait(c->matchByBeanName(address, tmp, beanName, tagC, c));
//		if((address[0] ==null || address[1]==null)
//				&& (tmp[0] == null || tmp[1]==null)) {
//			
//			JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(springConfigClass))
//			.setError((c,e)->{
//				log.error(c+"处理异常",e);
//			})
//			.setException((c,e)->{
//				log.error(c+"处理异常",e);
//			})
//			.runAndWait(c->matchByBeanName(address, tmp, beanName, tagC, c));
//		}
//		if((address[0] ==null || address[1]==null)) {
//			return tmp;
//		}
//		return address;
	}
	
//	private void matchByBeanName(Object[] address, Object[] tmp, final String beanName, final Class<?> tagC, Class<?> c) {
//		Method[] methods = c.getDeclaredMethods();
//		for(Method m : methods) {
//			Bean beanA = m.getAnnotation(Bean.class);
//			if(beanA != null) {
//			    String[] beanNames = beanA.value();
//		        for(String bn : beanNames) {
//		            if(Objects.equals(bn, beanName)) {
//		                address[0]=c;
//		                address[1]=m;
//		                break;
//		            }
//		        }
//		        if(m.getName().equals(beanName) && tagC!=null) {
//		        	if(ScanUtil.isExtends(m.getReturnType(), tagC) || ScanUtil.isImple(m.getReturnType(), tagC) || m.getReturnType() == tagC) {
//						tmp[0] = c;
//						tmp[1] = m;
//						break;
//					}
//		        }
//			}
//		}
//	}
	
	public void checkConfig(String className) {
		Lists.newArrayList(thridAutoConfigClass).forEach(config->{
			if(config.getName().contains(className)) {
				log.info("checked=>>>{}",className);
			}
		});
	}
	
	public JunitMethodDefinition findClassMethodByResultType(AssemblyDTO assemblyData) {
//		Object[] tmp = new Object[2];
		final Class<?> tagC = assemblyData.getTagClass();
//		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(customizeConfigClass))
//		.setError((c,e)->{
//			log.error(c+"处理异常",e);
//		})
//		.setException((c,e)->{
//			log.error(c+"处理异常",e);
//		})
//		.runAndWait(c->matchByClass(tmp, tagC, c));
//		if(tmp[0] ==null || tmp[1]==null) {
//			JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(springConfigClass))
//			.setError((c,e)->{
//				log.error(c+"处理异常",e);
//			})
//			.setException((c,e)->{
//				log.error(c+"处理异常",e);
//			})
//			.runAndWait(c->matchByClass(tmp, tagC, c));
//		}
//		return tmp;
		
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
				log.info("查看问题");
			}
			if(ScanUtil.isExtends(jmd.getReturnType(), tagC) || ScanUtil.isImple(jmd.getReturnType(), tagC) || jmd.getReturnType() == tagC) {
				return jmd;
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
	
	
//	private void matchByClass(Object[] tmp, final Class<?> tagC, Class<?> c) {
////		if(c.getName().contains("ElasticSearchClientConfiguration")) {
////			log.info("断点");
////		}
//		Method[] methods = c.getDeclaredMethods();
//		for(Method m : methods) {
//			Bean beanA = m.getAnnotation(Bean.class);
//			if(beanA != null) {
//				if(tagC!=null) {
//					if(!tagC.isInterface() && m.getReturnType().isInterface()) {
//						continue;
//					}
//					if(ScanUtil.isExtends(m.getReturnType(), tagC) || ScanUtil.isImple(m.getReturnType(), tagC) || m.getReturnType() == tagC) {
//						tmp[0] = c;
//						tmp[1] = m;
//						break;
//					}
//					if(ScanUtil.isImple(m.getReturnType(), FactoryBean.class)) {
//						Class<?> resultType =  m.getReturnType();
//						try {
//							if(resultType.getMethod("getObject").getReturnType() == tagC) {
//								tmp[0] = c;
//								tmp[1] = m;
//								break;
//							}
//						} catch (NoSuchMethodException | SecurityException e) {
//							throw new JunitException(e);
//						}
//					}
//				}
//			}
//		}
//	}
	
	public JunitMethodDefinition findCreateBeanFactoryClass(AssemblyDTO assemblyData) {
		registMethodBeanDefinition();
		if(StringUtils.isNotBlank(assemblyData.getBeanName())) {
			JunitMethodDefinition result = findClassMethodByBeanName(assemblyData);
			return result;
		}
		return findClassMethodByResultType(assemblyData);
	}
	
	private volatile Set<Class<?>> thridAutoConfigClass = Sets.newConcurrentHashSet();
	private volatile Set<Class<?>> customizeConfigClass = Sets.newConcurrentHashSet();
	private volatile Set<Class<?>> springConfigClass = Sets.newConcurrentHashSet();
	public void loadConfigurationClass(Class<?> configClass) {
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
	public JunitMethodDefinition findCreateBeanFactoryClass(Class<?> tagC) {
		registMethodBeanDefinition();
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
			Method[] methods = configClass.getDeclaredMethods();
			for(Method m : methods) {
				
				if(AnnotatedElementUtils.hasAnnotation(m, Bean.class)) {
					
					String beanName = determineBeanNameFor(m);
					
					JunitMethodDefinition jmd = new JunitMethodDefinition();
					jmd.setBeanName(beanName);
					jmd.setMethod(m);
					jmd.setReturnType(m.getReturnType());
					jmd.setConfigurationClass(configClass);
				
					customizeJmds.add(jmd);
					
					beanJmdMap.put(beanName, jmd);
				}
			}
		});
		JunitCountDownLatchUtils.buildCountDownLatch(Lists.newArrayList(springConfigClass))
		.setError((c,e)->log.error(c+"处理异常",e))
		.setException((c,e)->log.error(c+"处理异常",e))
		.runAndWait(configClass->{
			
			Method[] methods = configClass.getDeclaredMethods();
			for(Method m : methods) {
				
				if(AnnotatedElementUtils.hasAnnotation(m, Bean.class)) {
					
					String beanName = determineBeanNameFor(m);
					
					JunitMethodDefinition jmd = new JunitMethodDefinition();
					jmd.setBeanName(beanName);
					jmd.setMethod(m);
					jmd.setReturnType(m.getReturnType());
					jmd.setConfigurationClass(configClass);
				
					springBootJmds.add(jmd);
					
					beanJmdMap.putIfAbsent(beanName, jmd);
				}
			}
			
		});
		init = 2;
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
}
