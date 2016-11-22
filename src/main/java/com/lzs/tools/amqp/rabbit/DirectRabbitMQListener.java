package com.lzs.tools.amqp.rabbit;

import org.apache.log4j.Logger;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.InitializingBean;

import com.lzs.tools.amqp.FastJsonMQMessageConverter;

public abstract class DirectRabbitMQListener<T> extends RabbitMQListener<T> implements MessageListener,InitializingBean{
	private Logger logger = Logger.getLogger(DirectRabbitMQListener.class);
	
	private String[] queueNames;
	
	public DirectRabbitMQListener(String[] queueNames){
		super();
		this.queueNames = queueNames;
	}
	
	public String[] getQueues(){
		return queueNames;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if(rabbitAdmin == null){
			logger.error("无法注入rabbitAdmin实例");
			throw new Exception("can not inject 'rabbitAdmin' instance");
		}
		if(queueNames == null || queueNames.length == 0){
			logger.error("消息队列监听器没有关联到任何队列上");
			throw new Exception("mqlistener must bind to a queue!");
		}
		for(String q:queueNames){
			//init listener
			//listenerAdapter
			MessageListenerAdapter mla = new MessageListenerAdapter(this);
			mla.setMessageConverter(new FastJsonMQMessageConverter());
			SimpleMessageListenerContainer sc = new SimpleMessageListenerContainer();
			sc.setQueueNames(q);
			sc.setMessageListener(mla);
			sc.setConnectionFactory(rabbitAdmin.getRabbitTemplate().getConnectionFactory());
			sc.start();
		}
	}
}
