package com.github.jklasd.ct;

import org.junit.jupiter.api.Test;

import com.github.jklasd.test.TestUtil;
import com.github.jklasd.test.common.util.MethodSnoopUtil;

import javassist.CannotCompileException;

public class CtTest {

	
	@Test
	public void findNotPublicMethod() {
		try {
			MethodSnoopUtil.findNotPublicMethodForClass(TestUtil.class.getDeclaredMethod("getInstance"));
		} catch (NoSuchMethodException | SecurityException | CannotCompileException e) {
			e.printStackTrace();
		}
	}
}
