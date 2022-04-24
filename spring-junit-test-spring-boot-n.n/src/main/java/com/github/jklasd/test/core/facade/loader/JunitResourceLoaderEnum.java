package com.github.jklasd.test.core.facade.loader;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum JunitResourceLoaderEnum {

	ANN("spring.factories",1),
	JUNIT("scan.comp",2),
	XML("spring.handlers",3)
	;
	
	private String fileName;
	
	private Integer order;
}
