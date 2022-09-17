package com.github.jklasd.test.exception;

import lombok.Getter;

public class JunitException extends RuntimeException {

	@Getter
	private boolean need_throw;
	/**
	 *
	 */
	private static final long serialVersionUID = 5508124933812561856L;

	public JunitException() {
		super("============动作执行处理异常============");
	}

	public JunitException(String message) {
		this(message, false);
	}

	public JunitException(Throwable e) {
		super(e);
	}

	public JunitException(Throwable e, boolean need_throw) {
		super(e);
		this.need_throw = need_throw;
	}

	public JunitException(String message, boolean need_throw) {
		super(message);
		this.need_throw = need_throw;
	}
	
	public JunitException(String message,Throwable e) {
		super(message,e);
	}
	public JunitException(String message,Throwable e, boolean need_throw) {
		super(message,e);
		this.need_throw = need_throw;
	}
}
