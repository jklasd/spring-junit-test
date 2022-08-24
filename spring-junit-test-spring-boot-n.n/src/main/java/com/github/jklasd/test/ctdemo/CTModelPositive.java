package com.github.jklasd.test.ctdemo;

import java.util.List;

import lombok.Data;

@Data
public class CTModelPositive extends AbstractModelExt{
	private List<ModelPoint<CTModelPositive>> nextTrace;
}