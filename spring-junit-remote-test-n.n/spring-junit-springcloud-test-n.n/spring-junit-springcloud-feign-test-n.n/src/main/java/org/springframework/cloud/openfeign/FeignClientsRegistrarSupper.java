package org.springframework.cloud.openfeign;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;

import lombok.Getter;

public class FeignClientsRegistrarSupper {
	
	private FeignClientsRegistrar feignClientsRegistrar = new FeignClientsRegistrar();
	
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		feignClientsRegistrar.setEnvironment(LazyApplicationContext.getInstance().getEnvironment());
		
		//FeignContext
		
		registerDefaultConfiguration(metadata, registry);
		registerFeignClients(metadata, registry);
	}
	
	@Getter
	private LinkedHashSet<BeanDefinition> candidateComponents = new LinkedHashSet<>();
	
	public void registerFeignClients(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		
		Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableFeignClients.class.getName());
		final Class<?>[] clients = attrs == null ? null : (Class<?>[]) attrs.get("clients");
		if (clients == null || clients.length == 0) {
			ClassPathScanningCandidateComponentProvider scanner = feignClientsRegistrar.getScanner();
//			scanner.setResourceLoader(feignClientsRegistrar.resourceLoader);
			scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class));
			Set<String> basePackages = feignClientsRegistrar.getBasePackages(metadata);
			for (String basePackage : basePackages) {
				candidateComponents.addAll(scanner.findCandidateComponents(basePackage));
			}
		}
		else {
			for (Class<?> clazz : clients) {
				candidateComponents.add(new AnnotatedGenericBeanDefinition(clazz));
			}
		}
	}
	
	public void registerDefaultConfiguration(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		Map<String, Object> defaultAttrs = metadata.getAnnotationAttributes(EnableFeignClients.class.getName(), true);

		if (defaultAttrs != null && defaultAttrs.containsKey("defaultConfiguration")) {
			String name;
			if (metadata.hasEnclosingClass()) {
				name = "default." + metadata.getEnclosingClassName();
			}
			else {
				name = "default." + metadata.getClassName();
			}
			registerClientConfiguration(registry, name, defaultAttrs.get("defaultConfiguration"));
		}
	}
	public void registerClientConfiguration(BeanDefinitionRegistry registry, Object name, Object configuration) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FeignClientSpecification.class);
		builder.addConstructorArgValue(name);
		builder.addConstructorArgValue(configuration);
		registry.registerBeanDefinition(name + "." + FeignClientSpecification.class.getSimpleName(),
				builder.getBeanDefinition());
	}
	
	public Object registerFeignClient(BeanDefinitionRegistry registry, AnnotationMetadata annotationMetadata,
			Map<String, Object> attributes) {
		String className = annotationMetadata.getClassName();
		Class clazz = ClassUtils.resolveClassName(className, null);
		ConfigurableBeanFactory beanFactory = registry instanceof ConfigurableBeanFactory
				? (ConfigurableBeanFactory) registry : null;
		String contextId = getContextId(beanFactory, attributes);
		String name = feignClientsRegistrar.getName(attributes);
		FeignClientFactoryBean factoryBean = new FeignClientFactoryBean();
		factoryBean.setBeanFactory(beanFactory);
		factoryBean.setName(name);
		factoryBean.setContextId(contextId);
		factoryBean.setType(clazz);
		factoryBean.setRefreshableClient(isClientRefreshEnabled());
		factoryBean.setUrl(getUrl(beanFactory, attributes));
		factoryBean.setPath(getPath(beanFactory, attributes));
		factoryBean.setDecode404(Boolean.parseBoolean(String.valueOf(attributes.get("decode404"))));
		Object fallback = attributes.get("fallback");
		if (fallback != null) {
			factoryBean.setFallback(fallback instanceof Class ? (Class<?>) fallback
					: ClassUtils.resolveClassName(fallback.toString(), null));
		}
		Object fallbackFactory = attributes.get("fallbackFactory");
		if (fallbackFactory != null) {
			factoryBean.setFallbackFactory(fallbackFactory instanceof Class ? (Class<?>) fallbackFactory
					: ClassUtils.resolveClassName(fallbackFactory.toString(), null));
		}
		return factoryBean.getObject();

//		String[] qualifiers = getQualifiers(attributes);
//		if (ObjectUtils.isEmpty(qualifiers)) {
//			qualifiers = new String[] { contextId + "FeignClient" };
//		}
//
//		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, qualifiers);
//		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);

//		registerOptionsBeanDefinition(registry, contextId);
	}
	private boolean isClientRefreshEnabled() {
		return LazyApplicationContext.getInstance().getEnvironment().getProperty("feign.client.refresh-enabled", Boolean.class, false);
	}
	private String getUrl(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
		String url = resolve(beanFactory, (String) attributes.get("url"));
		return FeignClientsRegistrar.getUrl(url);
	}

	private String getPath(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
		String path = resolve(beanFactory, (String) attributes.get("path"));
		return FeignClientsRegistrar.getPath(path);
	}
	private void validate(Map<String, Object> attributes) {
		AnnotationAttributes annotation = AnnotationAttributes.fromMap(attributes);
		// This blows up if an aliased property is overspecified
		// FIXME annotation.getAliasedString("name", FeignClient.class, null);
		FeignClientsRegistrar.validateFallback(annotation.getClass("fallback"));
		FeignClientsRegistrar.validateFallbackFactory(annotation.getClass("fallbackFactory"));
	}
	private String getContextId(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
		String contextId = (String) attributes.get("contextId");
		if (!StringUtils.hasText(contextId)) {
			return feignClientsRegistrar.getName(attributes);
		}

		contextId = resolve(beanFactory, contextId);
		return FeignClientsRegistrar.getName(contextId);
	}

	private String resolve(ConfigurableBeanFactory beanFactory, String value) {
		if (StringUtils.hasText(value)) {
			if (beanFactory == null) {
				return LazyApplicationContext.getInstance().getEnvironment().resolvePlaceholders(value);
			}
			BeanExpressionResolver resolver = beanFactory.getBeanExpressionResolver();
			String resolved = beanFactory.resolveEmbeddedValue(value);
			if (resolver == null) {
				return resolved;
			}
			return String.valueOf(resolver.evaluate(resolved, new BeanExpressionContext(beanFactory, null)));
		}
		return value;
	}

	public String getClientName(Map<String, Object> attributes) {
		return feignClientsRegistrar.getName(attributes);
	}
}
