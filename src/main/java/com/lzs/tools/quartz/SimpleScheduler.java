package com.lzs.tools.quartz;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * 定时任务通用调度，没什么特别需求的可以直接用这个
 * @author hzlizhaosheng
 *
 */
public class SimpleScheduler extends SpringBeanJobFactory implements InitializingBean{

	private static final Logger logger = LoggerFactory.getLogger(SimpleScheduler.class);
	
//	@Autowired
//	private SchedulerFactoryBean schedulerFactoryBean;
	
//	@Autowired
	private Scheduler scheduler; 

	public Scheduler getScheduler() {
		return scheduler;
	}
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	@Autowired
	private AutowireCapableBeanFactory beanFactory;
	
	@Override
	public void afterPropertiesSet() throws Exception {
//		if(schedulerFactoryBean == null){
//			throw new Exception("'schedulerFactoryBean' can not be null.");
//		}
//		schedulerFactoryBean.setJobFactory(this);
		if(scheduler == null){
			throw new Exception("'schedulerFactoryBean' can not be null.");
		}
		scheduler.setJobFactory(this);
	}
	
	/**
	 * extends SpringBeanJobFactory 用于创建quartz的job实例，并自动注入job实例中的依赖
	 */
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception{
		Object job = super.createJobInstance(bundle);
		beanFactory.autowireBean(job);
		return job;
	}

	public void addJob(String identify,Class<? extends Job> jobClass, String cronExpression, Object obj) {
		try {
			// 调度器
//			Scheduler scheduler = schedulerFactoryBean.getScheduler();
			
			//获取触发器标识
	        TriggerKey triggerKey = TriggerKey.triggerKey("job:" + identify, Scheduler.DEFAULT_GROUP);
	        //获取触发器trigger
	        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
	         
	        if(null==trigger){//不存在任务
	            //创建任务
	            JobDetail jobDetail = JobBuilder.newJob(jobClass)
	                    .withIdentity("job:" + identify, Scheduler.DEFAULT_GROUP)
	                    .build();

	            jobDetail.getJobDataMap().put("data", obj);
	             
	            //表达式调度构建器
	            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
	             
	            //按新的cronExpression表达式构建一个新的trigger
	            trigger = TriggerBuilder.newTrigger()
	                    .withIdentity("job:" + identify, Scheduler.DEFAULT_GROUP)
	                    .withSchedule(scheduleBuilder)
	                    .build();

				scheduler.scheduleJob(jobDetail, trigger);
	        }
		} catch (SchedulerException e) {
			logger.error("创建定时任务失败，" + identify , e);
		}
         
	}
	     
	/**
	 * 移除一个任务(使用默认的任务组名，触发器名，触发器组名)
	 * 
	 * @param jobName
	 * @throws SchedulerException
	 */
	public void removeJob(String jobName) {
		try {
			// 获取触发器标识
			JobKey jobKey = JobKey.jobKey("job:" + jobName, Scheduler.DEFAULT_GROUP);
			scheduler.deleteJob(jobKey);
		} catch (SchedulerException e) {
			logger.error("移除定时任务失败，" + jobName, e);
		}
	}
	 
}
