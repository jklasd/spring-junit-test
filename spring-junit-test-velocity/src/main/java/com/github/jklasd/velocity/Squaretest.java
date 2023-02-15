package com.github.jklasd.velocity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.github.jklasd.test.common.JunitClassLoader;
import com.github.jklasd.velocity.util.JunitStringUtil;
import com.github.jklasd.velocity.util.MethodParamUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class Squaretest {
	
	public static void square(String packagePath) throws IOException{
		
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

        
        
        vc.put("methods", Lists.newArrayList(tagClass.getDeclaredMethods()));
        
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
        
      File file = new File(filePath+"/"+tagClass.getPackage().getName().replace(".", "/")+"/"+className+"_Test.java");
      
      if(!file.getParentFile().exists()) {
    	  file.getParentFile().mkdirs();
      }
      
      FileWriter fileWriter = new FileWriter(file);
      fileWriter.append(r);
      fileWriter.flush();
      fileWriter.close();
	}
	
	public static void main(String[] args) throws IOException {
		
		


	}
}
