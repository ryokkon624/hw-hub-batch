package com.hwhub.batch.config;

import com.hwhub.batch.application.service.NotificationAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class NotificationAggregationJobConfig {

  public static final String JOB_NAME = "notificationAggregationJob";
  public static final String STEP_NAME = "notificationAggregationStep";

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final NotificationAggregationService service;

  @Bean
  public Job notificationAggregationJob() {
    return new JobBuilder(JOB_NAME, jobRepository).start(notificationAggregationStep()).build();
  }

  @Bean
  public Step notificationAggregationStep() {
    return new StepBuilder(STEP_NAME, jobRepository)
        .tasklet(
            (contribution, chunkContext) -> {
              NotificationAggregationService.Result r =
                  service.aggregateTaskAssignmentNotifications();
              log.info(
                  "NotificationAggregationJob finished. claimed={}, notificationsInserted={}, eventsDone={}",
                  r.claimedCount(),
                  r.notificationsInserted(),
                  r.eventsDone());
              return RepeatStatus.FINISHED;
            },
            transactionManager)
        .build();
  }
}
