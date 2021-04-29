package com.github.jklasd.test.mq;

import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.Element;

import com.github.jklasd.test.AssemblyUtil;
import com.github.jklasd.test.InvokeUtil;
import com.github.jklasd.test.LazyBean;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.TestUtil;
/**
 * 构建rabbit应用
 * @author jubin.zhang
 *
 */
public class LazyRabbitMQBean extends LazyMQBean{
    LazyRabbitMQBean() {}
    private static LazyRabbitMQBean bean = new LazyRabbitMQBean();
    public static LazyRabbitMQBean getInstance() {
        return bean;
    }
    private Class<?> AmqpAdminC = ScanUtil.loadClass("org.springframework.amqp.core.AmqpAdmin");
    private Class<?>  RabbitTemplateC = ScanUtil.loadClass("org.springframework.amqp.rabbit.core.RabbitTemplate");
    private Class<?>  ConnectionFactoryC = ScanUtil.loadClass("com.rabbitmq.client.ConnectionFactory");
    private Class<?>  RabbitAdminC = ScanUtil.loadClass("org.springframework.amqp.rabbit.core.RabbitAdmin");
    private Class<?>  RabbitMessagingTemplateC = ScanUtil.loadClass("org.springframework.amqp.rabbit.core.RabbitMessagingTemplate");
    
	public Object buildBeanProcess(Class<?> classBean) throws InstantiationException, IllegalAccessException {
		
		if(factory != null) {
			if(classBean == AmqpAdminC) {
				if(admin == null) {
					try {
                        admin = RabbitAdminC.getConstructors()[0].newInstance(
                            InvokeUtil.invokeMethod(buildRabbitTemplate(),"getConnectionFactory"));
                    } catch (IllegalArgumentException | InvocationTargetException | SecurityException e) {
                         e.printStackTrace();
                    }
					//new RabbitAdmin(template.getConnectionFactory());
				}
				return admin;
			}else if(classBean == RabbitMessagingTemplateC) {
				Object objM = RabbitMessagingTemplateC.newInstance();
				InvokeUtil.invokeMethod(objM, "setRabbitTemplate", buildRabbitTemplate());
				return objM;
			}else if(classBean == RabbitTemplateC) {
				if(template == null) {
					buildRabbitTemplate();
				}
				return template;
			}
		}else {
			AssemblyUtil assemblyData = new AssemblyUtil();
			assemblyData.setTagClass(classBean);
			Object obj = LazyBean.findCreateBeanFromFactory(assemblyData);
			if(obj == null) {
				assemblyData.setNameMapTmp(ScanUtil.findClassMap("org.springframework.boot.autoconfigure.amqp"));
				obj = LazyBean.findCreateBeanFromFactory(assemblyData);
				return obj;
			}
			return obj;
		}
		return null;
	}
	private Object template = null;
	private Object factory;
	private Object buildRabbitTemplate() throws InstantiationException, IllegalAccessException {
		if(factory != null) {
		    if(template == null) {
		        template = RabbitTemplateC.newInstance(); 
		    }
			InvokeUtil.invokeMethod(template, "setConnectionFactory", factory);
		}
		return template;
	}
	
	public void loadConfig(Element contextAttr) throws InstantiationException, IllegalAccessException {
		if(factory == null) {
			// 定义一个连接工厂
			factory = ConnectionFactoryC.newInstance();
			// 设置服务端地址（域名地址/ip）
			InvokeUtil.invokeMethod(factory, "setHost", TestUtil.getPropertiesValue(contextAttr.getAttribute("host")));
			// 设置服务器端口号
			InvokeUtil.invokeMethod(factory, "setPort", Integer.valueOf(TestUtil.getPropertiesValue(contextAttr.getAttribute("port"),"5672")));
			// 设置虚拟主机(相当于数据库中的库)
			InvokeUtil.invokeMethod(factory, "setVirtualHost", "/");
			// 设置用户名
			InvokeUtil.invokeMethod(factory, "setUsername", TestUtil.getPropertiesValue(contextAttr.getAttribute("username")));
			// 设置密码
			InvokeUtil.invokeMethod(factory, "setPassword", TestUtil.getPropertiesValue(contextAttr.getAttribute("password")));
		}
	}
}
