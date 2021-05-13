package com.github.jklasd.test.beanfactory;

import java.lang.reflect.Type;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;

import lombok.Data;

@Data
public class BeanModel {
    private Class<?> tagClass;
    private String beanName;
    private MutablePropertyValues propValue;
    private Type[] classGeneric;
    private boolean xmlBean;
    private Map<String,String> beanMethods;
}
