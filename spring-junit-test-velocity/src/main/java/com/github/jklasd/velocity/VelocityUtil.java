package com.github.jklasd.velocity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.core.io.Resource;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.test.common.exception.JunitException;
import com.github.jklasd.test.lazyplugn.spring.LazyApplicationContext;
import com.github.jklasd.velocity.util.JunitStringUtil;
import com.github.jklasd.velocity.util.MethodNameUtil;
import com.github.jklasd.velocity.util.MethodParamUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VelocityUtil {

	private final static String junit_test = "";

	public static void square(String packagePath) throws IOException {
		if (packagePath.contains("/") || packagePath.endsWith(".") || packagePath.startsWith(".")) {
			throw new JunitException("包路径不合法", true);
		}
		Resource[] resources = LazyApplicationContext.getInstance()
				.getResources("classpath*:/" + packagePath.replace(".", "/") + "/*");

		for (Resource r : resources) {
			String fileName = r.getFilename();
			if (!fileName.endsWith(".class") || fileName.contains("junit_test") || fileName.endsWith("_Test.class")
					|| fileName.contains("$")) {
				continue;
			}
			try {
				Class<?> tagClass = JunitClassLoader.getInstance()
						.loadClass(packagePath.replace("/", ".") + "." + fileName.replace(".class", ""));
				if(tagClass.isInterface()) {
					continue;
				}
				square(tagClass, "src/test/java");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public static void square(Class<?> tagClass, String filePath) throws IOException {
		VelocityEngine ve = new VelocityEngine();// velocity的引擎
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.init();

		Template template = ve.getTemplate("junit-test.ft","UTF-8");// 模版，生成代码关键类


		VelocityContext vc = new VelocityContext();// 用于添加模版中的数据
		Map<String, String> sourceClass = Maps.newHashMap();
		vc.put("sourceClass", sourceClass);

		vc.put("springController", "true");

		vc.put("StringUtils", StringUtils.class);
		vc.put("filePath", filePath);
		vc.put("MethodParamUtil", MethodParamUtil.class);
		vc.put("JunitStringUtil", JunitStringUtil.class);

		String className = tagClass.getSimpleName();
		sourceClass.put("packageName", tagClass.getPackage().getName());
		sourceClass.put("name", className);

		sourceClass.put("testClassMemberName", JunitStringUtil.firstToSmall(className));
		
		List<Method> methods = Lists.newArrayList(tagClass.getDeclaredMethods()).stream()
			.filter(method -> Modifier.isPublic(method.getModifiers()) && !Modifier.isNative(method.getModifiers()))
			.sorted((m1,m2)->{return (m1.getName().hashCode()+m1.getParameterCount())-(m2.getName().hashCode()+m2.getParameterCount());})
			.collect(Collectors.toList());
		
		Map<String,String> methodNames = Maps.newHashMap();
		Set<String> sets = Sets.newHashSet();
		methods.forEach(method->{
			String tmpName = MethodNameUtil.convertionMethodName(method, sets);
			methodNames.put(MethodNameUtil.convertionMethodName(method), tmpName);
			
		});
		vc.put("methods", methods);
		vc.put("methodNames", methodNames);

		try {
			Class<?> testAnn = JunitClassLoader.getInstance().loadClass("org.junit.jupiter.api.Test");
			if (testAnn != null) {
				vc.put("junitAnn", "@RunSpringJunitTestFor5");
			} else {
				vc.put("junitAnn", "@RunWith(RunSpringJunitTest.class)");
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		StringWriter sw = new StringWriter();// 输出代码类

		template.merge(vc, sw);
		String r = sw.toString();
//		System.out.println(r);
		

		File file = new File(filePath + "/" + (junit_test + tagClass.getPackage().getName()).replace(".", "/") + "/"
				+ className + "_Test.java");

		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}else {
			if(file.exists()) {
				
				List<String> orgins = null;
				try {
					orgins = Files.readAllLines(file.toPath());
				} catch (IOException e) {
				}
				if(orgins!=null) {
					List<String> finalOrgins = orgins;
					/**
					 * 写入前对比
					 */
					List<Method> stillMethod = methods.stream().filter(m->MethodNameUtil.validateMethod(m,methodNames)).collect(Collectors.toList());
					Map<String,List<String>> methodContextMap = Maps.newHashMap();
					stillMethod.forEach(method->{
						String realName = methodNames.get(MethodNameUtil.convertionMethodName(method))+"_test";
						log.info("保留方法->{}",realName);
						List<String> methodContext = Lists.newArrayList();
						findMethodContext(realName, finalOrgins, methodContext);
						
						methodContextMap.put(realName, methodContext);
					});
					List<String> tagLines = Lists.newArrayList(r.replace("\r", "").split("\n"));
					
					methodContextMap.entrySet().forEach(entry->{
						String methodName = entry.getKey();
						int[] indexs = findMethodContext(methodName, tagLines,null);
						replaceLine(tagLines,indexs,entry.getValue());
						tagLines.remove(indexs[0]-2);//移除@DefaultTestMethod
					});
					
					StringBuilder builder = new StringBuilder();
					for(String line : tagLines) {
						builder.append(line+"\n");
					}
					r = builder.toString();
//					tagLines.forEach(code->{
//						log.info("code->{}",code);
//					});
				}
			}
		}

		FileWriter fileWriter = new FileWriter(file);
		fileWriter.append(r);
		fileWriter.flush();
		fileWriter.close();
		log.info("完成{}写入->{}", tagClass.getName(), file.getPath());
	}

	private static void replaceLine(List<String> defaultCodes, int[] indexs, List<String> stillCodes) {
		int s = 0;
		for(int i=indexs[0];i<indexs[1]+1;i++) {
			if(s<stillCodes.size()) {
				defaultCodes.set(i, stillCodes.get(s++));
			}else {
				break;
			}
		}
		for(;s<stillCodes.size();s++) {//原内容更多
			defaultCodes.add(indexs[1]+1, stillCodes.get(s));
		}
		for(int i=indexs[1];indexs[0]+s<=i;i--) {//原内容更短
			defaultCodes.remove(i);
		}
	}

	private static int[] findMethodContext(String methodName, List<String> tagLines,List<String> methodContext) {
		boolean start = false;
		int m = 0;
		int[] indexs = new int[2];
		for(int i=0;i<tagLines.size();i++) {
			String code = tagLines.get(i);
			if(code.indexOf(methodName)<0) {
				if(!start) {
					continue;
				}
				int s1 = code.indexOf("{");
				int s2 = code.lastIndexOf("{");
				if(s1==s2 && s1>0) {
					m++;
				}
				int s3 = code.indexOf("}");
				int s4 = code.lastIndexOf("}");
				if(s3==s4 && s3>0) {
					m--;
				}
			}else {
				m++;
				start = true;
				indexs[0] = i;
			}
			if(start && methodContext!=null) {
				methodContext.add(code);
			}
			if(m==0) {
				indexs[1] = i;
				break;
			}
		}
		return indexs;
	}

}
