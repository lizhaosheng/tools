package com.lzs.tools.amqp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.utils.SerializationUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.remoting.rmi.CodebaseAwareObjectInputStream;

import com.netease.numen.core.serialize.support.SerializableAdapter;
import com.netease.numen.core.util.ClassUtils;
import com.netease.numen.core.util.SpringUtils;

public class DefaultMQMessageConverter extends AbstractMessageConverter implements BeanClassLoaderAware {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultMQMessageConverter.class);
	
	public static final String DEFAULT_CHARSET = "UTF-8";

	private volatile String defaultCharset = DEFAULT_CHARSET;
	
	private String codebaseUrl;

	private ClassLoader beanClassLoader = ClassUtils.getClassLoader();
	
	private SerializableAdapter serializer = SpringUtils.getBean(SerializableAdapter.class);
	
	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void setDefaultCharset(String defaultCharset) {
		this.defaultCharset = (defaultCharset != null) ? defaultCharset : DEFAULT_CHARSET;
	}
	
	public void setCodebaseUrl(String codebaseUrl) {
		this.codebaseUrl = codebaseUrl;
	}
	
	/* 
	 * 反序列化：将amqp消息转换成java对象
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.amqp.support.converter.AbstractMessageConverter#fromMessage(org.springframework.amqp.core.Message)
	 */
	public Object fromMessage(Message message) throws MessageConversionException {
		Object content = null;
		MessageProperties properties = message.getMessageProperties();
		if (properties != null) {
			String contentType = properties.getContentType();
			if (contentType != null && contentType.startsWith("text")) {
				String encoding = properties.getContentEncoding();
				if (encoding == null) {
					encoding = this.defaultCharset;
				}
				try {
					content = new String(message.getBody(), encoding);
				}
				catch (UnsupportedEncodingException e) {
					logger.error("failed to convert text-based Message content", e);
					throw new MessageConversionException(
							"failed to convert text-based Message content", e);
				} 
			}
			else if (contentType != null &&
					contentType.equals(MessageProperties.CONTENT_TYPE_SERIALIZED_OBJECT)) {
				try {
					if(null != serializer){
						content = serializer.deserialize(message.getBody());
					}else{
						content = SerializationUtils.deserialize(createObjectInputStream(new ByteArrayInputStream(message.getBody()), this.codebaseUrl));
					}
				} catch (IOException e) {
					logger.error("failed to convert serialized Message content", e);
					throw new MessageConversionException(
							"failed to convert serialized Message content", e);
				} catch (IllegalArgumentException e) {
					logger.error("failed to convert serialized Message content", e);
					throw new MessageConversionException(
							"failed to convert serialized Message content", e);					
				}
			}
		}
		if (content == null) {
			content = message.getBody();
		}
		return content;
	}

	
	/* 
	 * 序列化：将对象序列化成amqp消息
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.amqp.support.converter.AbstractMessageConverter#createMessage(java.lang.Object, org.springframework.amqp.core.MessageProperties)
	 */
	protected Message createMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
		byte[] bytes = null;		
		if (object instanceof byte[]) {
			bytes = (byte[]) object;
			messageProperties.setContentType(MessageProperties.CONTENT_TYPE_BYTES);
		}
		else if (object instanceof String) {
			try {
				bytes = ((String) object).getBytes(this.defaultCharset);
			}
			catch (UnsupportedEncodingException e) {
				logger.error("failed to convert to Message content", e);
				throw new MessageConversionException(
						"failed to convert to Message content", e);
			}
			messageProperties.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
			messageProperties.setContentEncoding(this.defaultCharset);
		}
		else if (object instanceof Serializable) {
			try {
				if(null != serializer){
					bytes = serializer.serialize(object);
				}else{
					bytes = SerializationUtils.serialize(object);
				}
			} catch (IllegalArgumentException e) {
				logger.error("failed to convert to serialized Message content,check if the serialize and deserialize is the same serializer", e);
				throw new MessageConversionException(
						"failed to convert to serialized Message content,check if the serialize and deserialize is the same serializer", e);					
			}
			messageProperties.setContentType(MessageProperties.CONTENT_TYPE_SERIALIZED_OBJECT);
		}
		if (bytes != null) {
			messageProperties.setContentLength(bytes.length);
		}
		return new Message(bytes, messageProperties);
	}
	
	protected ObjectInputStream createObjectInputStream(InputStream is, String codebaseUrl) throws IOException {
		return new CodebaseAwareObjectInputStream(is, this.beanClassLoader, codebaseUrl);
	}
}
