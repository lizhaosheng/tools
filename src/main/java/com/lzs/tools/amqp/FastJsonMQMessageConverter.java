package com.lzs.tools.amqp;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;

import com.alibaba.fastjson.JSONObject;

public class FastJsonMQMessageConverter extends AbstractMessageConverter{
	private static final Logger logger = LoggerFactory.getLogger(FastJsonMQMessageConverter.class);
	
    public static final String DEFAULT_CHARSET = "UTF-8";
 
    private volatile String defaultCharset = DEFAULT_CHARSET;
     
    public FastJsonMQMessageConverter() {
        super();
    }
     
    public void setDefaultCharset(String defaultCharset) {
        this.defaultCharset = (defaultCharset != null) ? defaultCharset
                : DEFAULT_CHARSET;
    }
    
    /**
     * message转为string
     */
    public Object fromMessage(Message message) throws MessageConversionException {
        try {
            return new String(message.getBody(),defaultCharset);
        } catch (UnsupportedEncodingException e) {
        	logger.error("将消息转换为对象异常", e);
            e.printStackTrace();
        }
        return null;
    }

	@Override
    public Message createMessage(Object objectToConvert,
            MessageProperties messageProperties)
            throws MessageConversionException {
        byte[] bytes = null;
        try {
            String jsonString = JSONObject.toJSONString(objectToConvert);
            bytes = jsonString.getBytes(this.defaultCharset);
        } catch (UnsupportedEncodingException e) {
        	logger.error("将对象转换为json串异常", e);
            throw new MessageConversionException(
                    "Failed to convert Message content", e);
        } 
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(this.defaultCharset);
        if (bytes != null) {
            messageProperties.setContentLength(bytes.length);
        }
        return new Message(bytes, messageProperties);
 
    }
    
//	
//	public static void main(String[] arg){
//		
//		//推送消息
//		List<Integer> list = new ArrayList<Integer>();
//		list.add(1);
//		System.out.println(net.sf.json.JSONArray.fromObject(list));
//		
//		MQMessage message = new MQMessage();
//		message.addParm("list", list);
//		Message m = tomeaage(message,new MessageProperties());
//		
////		MQMessage obj = FastJsonMQMessageConverter.fromMessage(m, MQMessage.class , FastJsonMQMessageConverter.DEFAULT_CHARSET);
////		System.out.println(obj.getParm("list"));
//		
//		//message.addParm("companyID", companyID);
//		//message.addParm("bossStaffId", creator);	
////		rabbitTemplate.convertAndSend(Constants.MQ.R_VIPNOTICES, message);
//		
//		
//	}
}
