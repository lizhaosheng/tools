package com.lzs.tools.amqp.rabbit;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import com.alibaba.fastjson.JSON;
import com.lzs.tools.amqp.FastJsonMQMessageConverter;

public abstract class RabbitMQListener<T> implements MessageListener{
	private Logger logger = Logger.getLogger(RabbitMQListener.class);
//	
//	protected Class<T> entityClass;

	protected Type type;
	
	@Resource
	protected RabbitAdmin rabbitAdmin;
	
	@Resource
	protected RabbitTemplate rabbitTemplate;
	
	
	public RabbitMQListener(){
		type = ( (ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
//		if(type == null){
//			entityClass = (Class<T>) Object.class;
//		}
//		else if(type instanceof ParameterizedType){
//			entityClass = (Class<T>) ((ParameterizedType)type).getRawType();
//		}
//		else{
//			entityClass = (Class<T>) type;
//		}
	}
	@SuppressWarnings("unchecked")
	public final void onMessage(Message message){
		try{
			Object convertedMessage = extractMessage(message);
//		// Invoke the handler method with appropriate arguments.
//		Object[] listenerArguments = buildListenerArguments(convertedMessage);
//		Object result = invokeListenerMethod(methodName, listenerArguments, message);
			handleMessage((T) convertedMessage);
		}catch (Exception e){
			try {
				String json = new String(message.getBody(),FastJsonMQMessageConverter.DEFAULT_CHARSET);
				logger.error("解析mq消息为对象出错" + json, e);
			} catch (UnsupportedEncodingException e1) {
				logger.error("解析mq消息为字符串出错", e);
			}
		}
	}
	/**
	 * 获取rabbitTemplate中配置的消息转换对象，并进行消息解析
	 * @param message
	 * @return
	 */
	protected Object extractMessage(Message message) {
		MessageConverter converter = rabbitTemplate.getMessageConverter();
		if (converter != null) {
			String data = (String) converter.fromMessage(message);
			return JSON.parseObject(data, type);
		}
		return message;
	}
	
	public abstract void handleMessage(T t);

}
