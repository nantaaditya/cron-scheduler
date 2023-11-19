package com.nantaaditya.cronscheduler.configuration;

import com.nantaaditya.cronscheduler.listener.JobListener;
import com.nantaaditya.cronscheduler.listener.JobTriggerListener;
import com.nantaaditya.cronscheduler.listener.SchedulerListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class QuartzConfiguration {

  public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException {
    Scheduler scheduler = schedulerFactoryBean.getScheduler();
    scheduler.getListenerManager().addTriggerListener(new JobTriggerListener());
    scheduler.getListenerManager().addJobListener(new JobListener());
    scheduler.getListenerManager().addSchedulerListener(new SchedulerListener());
    return scheduler;
  }

  @Bean
  public SchedulerFactoryBean schedulerFactoryBean(ApplicationContext applicationContext) {
    SchedulerFactoryBean quartzScheduler = new SchedulerFactoryBean();
    quartzScheduler.setJobFactory(jobFactory(applicationContext));
    quartzScheduler.setSchedulerName("quartz-scheduler");

    return quartzScheduler;
  }

  public JobFactory jobFactory(ApplicationContext applicationContext) {
    AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
    jobFactory.setApplicationContext(applicationContext);
    return jobFactory;
  }

  public final class AutowiringSpringBeanJobFactory
      extends SpringBeanJobFactory
      implements ApplicationContextAware {

    private transient AutowireCapableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      beanFactory = applicationContext.getAutowireCapableBeanFactory();
    }

    @Override
    protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
      final Object job = super.createJobInstance(bundle);
      beanFactory.autowireBean(job);
      return job;
    }
  }
}
