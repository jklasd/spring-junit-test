package com.github.jklasd.test.mq;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.w3c.dom.Element;

import com.github.jklasd.test.AssemblyUtil;
import com.github.jklasd.test.LazyBean;
import com.github.jklasd.test.ScanUtil;
import com.github.jklasd.test.TestUtil;
import com.rabbitmq.client.ConnectionFactory;
/**
 * 构建rabbit应用
 * @author jubin.zhang
 *
 */
public class LazyRabbitMQBean extends LazyMQBean{
	@Override
	public Object buildBeanProcess(Class classBean) throws InstantiationException, IllegalAccessException {
		
		if(factory != null) {
			if(classBean == AmqpAdmin.class) {
				if(admin == null) {
					if(template == null) {
						buildRabbitTemplate();
					}
					admin = new RabbitAdmin(template.getConnectionFactory());
				}
				return admin;
			}else if(classBean == RabbitMessagingTemplate.class) {
				RabbitMessagingTemplate objM = (RabbitMessagingTemplate) classBean.newInstance();
				if(template == null) {
					buildRabbitTemplate();
				}
				objM.setRabbitTemplate(template);
				return objM;
			}else if(classBean == RabbitTemplate.class) {
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
	private RabbitTemplate template = null;
	private static ConnectionFactory factory;
	private void buildRabbitTemplate() throws InstantiationException, IllegalAccessException {
		if(factory != null) {
			this.template = RabbitTemplate.class.newInstance();
//		Connection connection = factory.newConnection();
			template.setConnectionFactory(new CachingConnectionFactory(factory));
		}
	}
	
	public static void loadConfig(Element contextAttr) {
		if(factory == null) {
			// 定义一个连接工厂
			factory = new ConnectionFactory();
			// 设置服务端地址（域名地址/ip）
			factory.setHost(TestUtil.getPropertiesValue(contextAttr.getAttribute("host")));
			// 设置服务器端口号
			factory.setPort(Integer.valueOf(TestUtil.getPropertiesValue(contextAttr.getAttribute("port"),"5672")));
			// 设置虚拟主机(相当于数据库中的库)
			factory.setVirtualHost("/");
			// 设置用户名
			factory.setUsername(TestUtil.getPropertiesValue(contextAttr.getAttribute("username")));
			// 设置密码
			factory.setPassword(TestUtil.getPropertiesValue(contextAttr.getAttribute("password")));
		}
	}
}
