package com.github.jklasd.test.common.model;

import java.lang.reflect.Type;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;

import lombok.Data;

@Data
public class BeanModel {
    private Class<?> tagClass;
    private String beanName;
    private MutablePropertyValues propValue;
    private ConstructorArgumentValues constructorArgValue;
    private Type[] classGeneric;
    private boolean xmlBean;
//    private Map<String,String> beanMethods;
    private ConstructorArgumentValues constructorArgs;
    private boolean isThrows;
}
