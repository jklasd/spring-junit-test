package com.github.jklasd.velocity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.Map;
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
import com.github.jklasd.velocity.util.MethodParamUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class Squaretest {
	
	private final static String junit_test = "";
	
	public static void square(String packagePath) throws IOException{
		if(packagePath.contains("/") || packagePath.endsWith(".") || packagePath.startsWith(".")) {
			throw new JunitException("包路径不合法",true);
		}
		Resource[] resources = LazyApplicationContext.getInstance().getResources("classpath*:/"+packagePath.replace(".", "/")+"/*");
		
		for(Resource r : resources) {
			String fileName  = r.getFilename();
			if(!fileName.endsWith(".class")
					|| fileName.contains("junit_test")
					|| fileName.endsWith("_Test.class")
					|| fileName.contains("$")) {
				continue;
			}
			try {
				Class<?> tagClass = JunitClassLoader.getInstance().loadClass(packagePath.replace("/", ".")+"."+fileName.replace(".class", ""));
				square(tagClass,"src/test/java");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public static void square(Class<?> tagClass,String filePath) throws IOException{
		VelocityEngine ve = new VelocityEngine();//velocity的引擎
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER,"classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();

        Template template = ve.getTemplate("junit-test.ft");//模版，生成代码关键类


        VelocityContext vc = new VelocityContext();//用于添加模版中的数据
        Map<String,String> sourceClass = Maps.newHashMap();
        vc.put("sourceClass", sourceClass);
        
        vc.put("springController", "true");
        
        vc.put("StringUtils", StringUtils.class);
        vc.put("MethodParamUtil", MethodParamUtil.class);
        vc.put("JunitStringUtil", JunitStringUtil.class);
        
        String className = tagClass.getSimpleName();
        sourceClass.put("packageName", tagClass.getPackage().getName());
        sourceClass.put("name", className);
        
        sourceClass.put("testClassMemberName", JunitStringUtil.firstToSmall(className));

        
        vc.put("methods", Lists.newArrayList(tagClass.getDeclaredMethods()).stream()
        		.filter(method->Modifier.isPublic(method.getModifiers()))
        		.collect(Collectors.toList()));
        
        try {
			Class<?> testAnn = JunitClassLoader.getInstance().loadClass("org.junit.jupiter.api.Test");
			if(testAnn != null) {
				vc.put("junitAnn", "@RunSpringJunitTestFor5");
			}else {
				vc.put("junitAnn", "@RunWith(RunSpringJunitTest.class)");
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
        
        StringWriter sw = new StringWriter();//输出代码类

        template.merge(vc,sw);
        String r = sw.toString();
        System.out.println(r);
        
      File file = new File(filePath+"/"+(junit_test+tagClass.getPackage().getName()).replace(".", "/")+"/"+className+"_Test.java");
      
      if(!file.getParentFile().exists()) {
    	  file.getParentFile().mkdirs();
      }
      
      FileWriter fileWriter = new FileWriter(file);
      fileWriter.append(r);
      fileWriter.flush();
      fileWriter.close();
	}
	
}
