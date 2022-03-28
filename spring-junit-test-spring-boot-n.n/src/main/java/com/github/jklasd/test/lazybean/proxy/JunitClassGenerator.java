//package com.github.jklasd.test.lazybean.proxy;
//
//import java.lang.reflect.Constructor;
//import java.lang.reflect.Modifier;
//
//import com.github.jklasd.test.core.facade.JunitClassLoader;
//import com.github.jklasd.test.exception.JunitException;
//
//import javassist.ClassPool;
//import javassist.CtClass;
//import javassist.CtConstructor;
//import javassist.CtNewConstructor;
//import javassist.LoaderClassPath;
//import javassist.NotFoundException;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public final class JunitClassGenerator {
//	/**
//     * void(V).
//     */
//    public static final char JVM_VOID = 'V';
//
//    /**
//     * boolean(Z).
//     */
//    public static final char JVM_BOOLEAN = 'Z';
//
//    /**
//     * byte(B).
//     */
//    public static final char JVM_BYTE = 'B';
//
//    /**
//     * char(C).
//     */
//    public static final char JVM_CHAR = 'C';
//
//    /**
//     * double(D).
//     */
//    public static final char JVM_DOUBLE = 'D';
//
//    /**
//     * float(F).
//     */
//    public static final char JVM_FLOAT = 'F';
//
//    /**
//     * int(I).
//     */
//    public static final char JVM_INT = 'I';
//
//    /**
//     * long(J).
//     */
//    public static final char JVM_LONG = 'J';
//
//    /**
//     * short(S).
//     */
//    public static final char JVM_SHORT = 'S';
//	private ClassPool mPool;
//    private CtClass mCtc;
//    private ClassLoader junitClassLoader;
//    private Class<?> tagClass;
//    private final static String jklasd = "$JKLASD";
//    private JunitClassGenerator(String className,Class<?> tagClass) {
//    	this.tagClass = tagClass;
//    	junitClassLoader = JunitClassLoader.getInstance();
//    	mPool = ClassPool.getDefault();
//    	mPool.appendClassPath(new LoaderClassPath(junitClassLoader));
//    	mCtc = mPool.makeClass(className);
//    }
//    
//    public static JunitClassGenerator getGeneratorInstance(Class<?> tagClass) {
//    	return new JunitClassGenerator(tagClass.getName()+jklasd,tagClass);
//    }
//    
//    public Class<?> makeClass(){
//    	try {
//    		mCtc.setSuperclass(mPool.getCtClass(tagClass.getName()));
//    		int count = 10;
//    		Constructor<?> tmp = null;
//    		for(Constructor<?> c : tagClass.getDeclaredConstructors()) {
//                if(c.getParameterCount()<count) {
//                	tmp = c;
//                }
//            }
//    		CtConstructor cc = getCtConstructor(tmp);
//    		cc = CtNewConstructor.copy(cc, mCtc, null);
//    		cc.setModifiers(Modifier.PUBLIC);
//    		mCtc.addConstructor(cc);
//    		
//    		return mCtc.toClass();
//		} catch (Exception e) {
//			log.warn("JunitClassGenerator#makeClass",e);
//			throw new JunitException("构建代理类异常",true);
//		}
//    }
//    
//    private CtConstructor getCtConstructor(Constructor<?> c) throws NotFoundException {
//    	String desc = getDesc(c);
//        return mPool.get(c.getDeclaringClass().getName()).getConstructor(desc);
//    }
//    
//    public static String getDesc(final Constructor<?> c) {
//        StringBuilder ret = new StringBuilder("(");
//        Class<?>[] parameterTypes = c.getParameterTypes();
//        for (int i = 0; i < parameterTypes.length; i++) {
//            ret.append(getDesc(parameterTypes[i]));
//        }
//        ret.append(')').append('V');
//        return ret.toString();
//    }
//    public static String getDesc(Class<?> c) {
//        StringBuilder ret = new StringBuilder();
//
//        while (c.isArray()) {
//            ret.append('[');
//            c = c.getComponentType();
//        }
//
//        if (c.isPrimitive()) {
//            String t = c.getName();
//            if ("void".equals(t)) {
//                ret.append(JVM_VOID);
//            } else if ("boolean".equals(t)) {
//                ret.append(JVM_BOOLEAN);
//            } else if ("byte".equals(t)) {
//                ret.append(JVM_BYTE);
//            } else if ("char".equals(t)) {
//                ret.append(JVM_CHAR);
//            } else if ("double".equals(t)) {
//                ret.append(JVM_DOUBLE);
//            } else if ("float".equals(t)) {
//                ret.append(JVM_FLOAT);
//            } else if ("int".equals(t)) {
//                ret.append(JVM_INT);
//            } else if ("long".equals(t)) {
//                ret.append(JVM_LONG);
//            } else if ("short".equals(t)) {
//                ret.append(JVM_SHORT);
//            }
//        } else {
//            ret.append('L');
//            ret.append(c.getName().replace('.', '/'));
//            ret.append(';');
//        }
//        return ret.toString();
//    }
//}