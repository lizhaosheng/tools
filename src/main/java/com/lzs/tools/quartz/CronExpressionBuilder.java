/** 
 * Project Name: kolibri-core 
 * File Name: CronExpressionBuilder.java 
 * Package Name: com.netease.kolibri.core.quartz 
 * Date: 2016年7月5日下午1:59:28 
 * Copyright (c) 2016, hzlizhaosheng@corp.netease.com All Rights Reserved. 
 * 
*/  

package com.lzs.tools.quartz;

import java.util.Calendar;
import java.util.Date;

/** 
 * ClassName: CronExpressionBuilder <br/> 
 * Describe: cron表达式构造器 <br/> 
 * Date: 2016年7月5日 下午1:59:28 <br/> 
 * @author lizhaosheng
 * @version 
 * @since JDK 1.6 
 * @see       
 */
public class CronExpressionBuilder {

	/**
	 * 根据时间转换成cron的时间表达式（包括年月日 时分秒）
	 * 
	 * @param date
	 * @return
	 */
	public static String getCronExpressionByDate(Date date) {
		if (date == null) {
			throw new NullPointerException("时间为空");
		}
		StringBuilder expression = new StringBuilder();
		Calendar time = Calendar.getInstance();
		time.setTime(date);

		int year = time.get(Calendar.YEAR);
		String week = "?";// 默认不使用周数
		int month = time.get(Calendar.MONTH) + 1;
		int day = time.get(Calendar.DAY_OF_MONTH);
		int hour = time.get(Calendar.HOUR_OF_DAY);
		int minute = time.get(Calendar.MINUTE);
		int second = time.get(Calendar.SECOND);

		expression.append(second).append(" " + minute).append(" " + hour).append(" " + day).append(" " + month).append(" " + week).append(" " + year);
		return expression.toString();
	}
	
}
