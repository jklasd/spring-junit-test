package com.github.jklasd.test.common.model;

import lombok.Data;

@Data
public class BeanInitModel extends BeanModel{

	private Object obj;
	
	private boolean isStatic;
}
