 package com.github.jklasd.test.exception;

 public class JunitException extends RuntimeException{

    /**
     *
     */
    private static final long serialVersionUID = 5508124933812561856L;
    public JunitException() {
        super("============动作执行处理异常============");
    }
    public JunitException(String message) {
        super(message);
    }
    public JunitException(Throwable e) {
        super(e);
    }
}
