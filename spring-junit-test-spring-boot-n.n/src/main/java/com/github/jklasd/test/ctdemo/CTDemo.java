package com.github.jklasd.test.ctdemo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

import com.alibaba.fastjson.JSONObject;
import com.github.jklasd.test.TestUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;

public class CTDemo {

	@Test
	public void test() throws NotFoundException, CannotCompileException {
		ClassPool cp = ClassPool.getDefault();
		
		CTModelPositive root = new CTModelPositive();
		String className = TestUtil.class.getName();
		root.setClassName(className);
		root.setNextTrace(Lists.newArrayList());
		CtClass ctClass = cp.get(className);
		for(CtMethod method : ctClass.getDeclaredMethods()) {
//			System.out.println("======================"+method.getName()+"======================");
			if(method.getName().startsWith("$")
					|| method.getName().startsWith("lambda")) {
				continue;
			}
			CTModelPositive model = new CTModelPositive();
			model.setClassName(className);
			model.setMethodName(method.getName());
			model.setSignature(method.getSignature());
			model.makeKey();
			root.getNextTrace().add(ModelPoint.build(-1, model));
			
			ExprEditor ee = new ExprEditorExt(model);
			method.instrument(ee);
		}

		System.out.println("等待执行结果");
		
		
//		List<CTModelReverse> list = buildCTR(root);
//		System.out.println(JSONObject.toJSONString(root));
		try {
			Thread.sleep(5*1000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		List<CTModelReverse> list = Lists.newArrayList();
		buildCTR(root,list);
		
		System.out.println("等待执行结果2");
//		try {
//			Thread.sleep(10*1000l);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
		System.out.println(JSONObject.toJSONString(list));
		File tmp = new File("result.txt");
		if(!tmp.exists()) {
			try {
				tmp.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			BufferedWriter bos = new BufferedWriter(new FileWriter(tmp));
			bos.write(JSONObject.toJSONString(list));
			bos.flush();
			bos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private Map<String,CTModelReverse> cache = Maps.newHashMap();
	protected void buildCTR(CTModelPositive root,List<CTModelReverse> list) {
		CTModel ttmp = new CTModel();
		BeanUtils.copyProperties(root, ttmp);
		
		CTModelReverse ctmTmp = new CTModelReverse();
		ctmTmp.setKey(ttmp.makeKey());
		if(ctmTmp.getKey() == null) {
			ctmTmp.setClassName(root.getClassName());
		}
		root.getNextTrace().forEach(item->{
			
			CTModel ctmr = new CTModel();
			ctmr.setClassName(item.getPoint().getClassName());
			ctmr.setMethodName(item.getPoint().getMethodName());
			ctmr.setSignature(item.getPoint().getSignature());
			if(!cache.containsKey(ctmr.makeKey())) {
				CTModelReverse tmp = new CTModelReverse();
				tmp.setKey(ctmr.makeKey());
				cache.put(tmp.getKey(), tmp);
				ModelPoint<CTModelReverse> point = ModelPoint.build(item.getParentLineNum(), ctmTmp);
				tmp.setParent(Lists.newArrayList(point));
				list.add(tmp);
			}else {
				CTModelReverse tmp = cache.get(ctmr.makeKey());
				String fixedKey = ctmTmp.getKey();
				boolean finded = tmp.getParent().stream().anyMatch(ite->Objects.equals(ite.getPoint().getKey(), fixedKey));
				if(!finded) {
					tmp.getParent().add(ModelPoint.build(item.getParentLineNum(), ctmTmp));
				}
			}
			if(item.getPoint().getNextTrace()!=null && !item.getPoint().getNextTrace().isEmpty()) {
				item.getPoint().getNextTrace().forEach(item2->{
					buildCTR(item.getPoint(), list);
				});
			}
		});
	}
}
