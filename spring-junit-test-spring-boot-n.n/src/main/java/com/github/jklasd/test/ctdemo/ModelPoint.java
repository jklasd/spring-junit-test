package com.github.jklasd.test.ctdemo;

import lombok.Getter;

public class ModelPoint<T>{
	@Getter
	protected Integer parentLineNum;
	@Getter
	protected T point;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends AbstractModel> ModelPoint build(Integer parentLineNum,T obj) {
		ModelPoint model = new ModelPoint();
		model.parentLineNum = parentLineNum;
		model.point = obj;
		return model;
	}
}
