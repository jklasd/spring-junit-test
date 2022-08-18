package com.github.jklasd.test.ctdemo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public abstract class AbstractModel {


	protected String className;
	
	protected String methodName;
	
	protected String signature;
	
	public String makeKey() {
		if(className!=null && methodName!=null && signature!=null ) {
			return className+"-"+methodName+"-"+signature;
		}else {
			log.warn("MAKE key 异常");
			return null;
		}
	}
}
