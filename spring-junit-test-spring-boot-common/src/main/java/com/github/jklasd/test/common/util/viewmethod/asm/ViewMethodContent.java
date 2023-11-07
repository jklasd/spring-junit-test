package com.github.jklasd.test.common.util.viewmethod.asm;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;

import com.github.jklasd.test.common.util.viewmethod.PryMethodInfoI;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ViewMethodContent {
	static Map<String,ClassReader> cacheReader = Maps.newConcurrentMap();
	
	public static PryMethodInfoI findNotPublicMethodForClass(Method method) {
//		getCtMethod(method);
		return null;
	}
	
	private static void getCtMethod(Method method) {
		try {
			String existKey = method.getDeclaringClass().getName();
			if(!cacheReader.containsKey(existKey)) {
				cacheReader.put(existKey, new ClassReader(method.getDeclaringClass().getName()));
			}
			ClassReader classReader = cacheReader.get(existKey);
	        //处理
			log.info("============监控begin============");
			
			classReader.accept(new ClassVisitorExt(method), ClassReader.SKIP_CODE);
			
			log.info("============监控end============");
		} catch (Exception e) {
//			throw new JunitException(e);
		}
	}
	
	static class ClassVisitorExt extends ClassVisitor{
		
		private Method method;

		public ClassVisitorExt(Method method) {
			super(Opcodes.ASM4);
			this.method = method;
		}
		public ClassVisitorExt(final int api, final ClassVisitor classVisitor) {
			super(api,classVisitor);
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
				String[] exceptions) {
			MethodVisitor mv =  super.visitMethod(access, name, descriptor, signature, exceptions);
			if(Objects.equal(method.getName(), name)) {
				log.info("查看详情");
			}
			return mv;
		}
	}
	
	static class MethodVisitorExt extends MethodVisitor{
		public MethodVisitorExt(int api) {
			super(api);
		}
		public MethodVisitorExt(int api, MethodVisitor methodVisitor) {
			super(api, methodVisitor);
		}


	}
}
