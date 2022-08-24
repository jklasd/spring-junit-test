package com.github.jklasd.test.ctdemo;

import com.google.common.collect.Lists;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class ExprEditorExt extends ExprEditor{
	CTModelPositive parent;
	ClassPool cp = ClassPool.getDefault();
	public ExprEditorExt(CTModelPositive parent) {
		if(parent.getNextTrace() == null) {
			parent.setNextTrace(Lists.newArrayList());
		}
		this.parent = parent;
	}
	
	public void edit(MethodCall m) throws CannotCompileException{
		String packagePath = "com.github.jklasd";//指定路径
		if(m.getClassName().contains(packagePath)
				&& !m.getMethodName().startsWith("$")
				) {
			CTModelPositive nextModel = new CTModelPositive();
			nextModel.setClassName(m.getClassName());
			nextModel.setMethodName(m.getMethodName());
			nextModel.setSignature(m.getSignature());
			nextModel.makeKey();
			parent.getNextTrace().add(ModelPoint.build(m.getLineNumber(), nextModel));
			
//			ThreadPoolUtils.commonRunLimit(()->{
//				try {
//					ExprEditor ee = new ExprEditorExt(nextModel);
//					m.getMethod().instrument(ee);
//				} catch (CannotCompileException | NotFoundException e) {
//					e.printStackTrace();
//				}
//			});
		}
	}
}
