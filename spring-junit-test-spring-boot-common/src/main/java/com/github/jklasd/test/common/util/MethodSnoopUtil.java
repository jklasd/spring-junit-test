package com.github.jklasd.test.common.util;

import java.lang.reflect.Method;

import com.github.jklasd.test.common.util.viewmethod.PryMethodInfoI;
import com.github.jklasd.test.common.util.viewmethod.javasist.ViewMethodContent;

import javassist.CannotCompileException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodSnoopUtil {
	
	
	public static PryMethodInfoI findNotPublicMethodForClass(Method method) throws CannotCompileException{
//		com.github.jklasd.test.common.util.viewmethod.asm.ViewMethodContent.findNotPublicMethodForClass(method);
		return ViewMethodContent.findNotPublicMethodForClass(method);
	}
	
}
