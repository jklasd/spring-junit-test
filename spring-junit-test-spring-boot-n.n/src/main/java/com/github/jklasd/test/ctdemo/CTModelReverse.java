package com.github.jklasd.test.ctdemo;

import java.util.List;

import lombok.Data;

@Data
public class CTModelReverse extends AbstractModelExt{
	private List<ModelPoint<CTModelReverse>> parent;
}
