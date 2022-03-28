package com.github.jklasd.test;

import lombok.Getter;

public class TestMockBean {

	@Getter
	private Integer value;
	private TestMockBean() {
	}
	private TestMockBean(Integer value) {
		this.value = value;
	}
	public void exec() {
		
	}
}
