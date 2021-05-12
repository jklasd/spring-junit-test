package com.github.jklasd.test.beanfactory;

import java.lang.reflect.Type;
import java.util.Map;

import lombok.Data;

@Data
public class BeanModel {
    private Class<?> tagClass;
    private String beanName;
    private Map<String, Object> attr;
    private Type[] classGeneric;
    private String beanClassName;
    private Map<String,String> beanMethods;
}
