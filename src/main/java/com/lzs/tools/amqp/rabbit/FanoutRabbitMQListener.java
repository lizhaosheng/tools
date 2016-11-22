package com.lzs.tools.amqp.rabbit;

import org.apache.log4j.Logger;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.InitializingBean;

import com.lzs.tools.amqp.FastJsonMQMessageConverter;

public abstract class FanoutRabbitMQListener<T>extends RabbitMQListener<T>  implements MessageListener,InitializingBean{
	private Logger logger = Logger.getLogger(FanoutRabbitMQListener.class);
	
	private String fanoutExchangeName;

	private Queue q = null;
	
	public FanoutRabbitMQListener(String fanoutExchangeName){
		super();
		this.fanoutExchangeName = fanoutExchangeName;
	}
	public FanoutRabbitMQListener(String fanoutExchangeName, Queue q){
		super();
		this.fanoutExchangeName = fanoutExchangeName;
		this.q = q;
	}
	
	
	public String getFanoutExchage(){
		return fanoutExchangeName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(rabbitAdmin == null){
			logger.error("无法注入rabbitAdmin实例");
			throw new Exception("can not inject 'rabbitAdmin' instance");
		}

		// 创建随机队列
		if(q == null){
			q = rabbitAdmin.declareQueue();
		}
		Binding binding = new Binding(q.getName(), DestinationType.QUEUE, fanoutExchangeName, "", null);
		
		binding.setAdminsThatShouldDeclare(this);
		rabbitAdmin.declareBinding(binding);
		
		//init listener
		//listenerAdapter
		MessageListenerAdapter mla = new MessageListenerAdapter(this);
		mla.setMessageConverter(new FastJsonMQMessageConverter());
		SimpleMessageListenerContainer sc = new SimpleMessageListenerContainer();
		sc.setQueueNames(q.getName());
		sc.setMessageListener(mla);
		sc.setConnectionFactory(rabbitAdmin.getRabbitTemplate().getConnectionFactory());
		sc.start();
	}
}
