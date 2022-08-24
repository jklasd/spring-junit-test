package com.github.jklasd.test.core.facade.loader;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum JunitResourceLoaderEnum {

	XML("spring.handlers",1),
	ANN("spring.factories",2),
	JUNIT("scan.comp",3),
	;
	
	private String fileName;
	
	private Integer order;
}
