package com.hwhub.batch.config;

import com.hwhub.batch.application.service.HouseworkTaskGenerateService;
import java.util.Objects;
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
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class HouseworkTaskGenerateJobConfig {

  public static final String JOB_NAME = "houseworkTaskGenerateJob";
  public static final String STEP_NAME = "houseworkTaskGenerateStep";

  @NonNull private final JobRepository jobRepository;
  @NonNull private final PlatformTransactionManager transactionManager;
  @NonNull private final HouseworkTaskGenerateService service;

  @Bean
  public Job houseworkTaskGenerateJob() {
    return new JobBuilder(JOB_NAME, jobRepository)
        .start(Objects.requireNonNull(houseworkTaskGenerateStep()))
        .build();
  }

  @Bean
  public Step houseworkTaskGenerateStep() {
    return new StepBuilder(STEP_NAME, jobRepository)
        .tasklet(
            (contribution, chunkContext) -> {
              int inserted = service.generateTasks();
              log.info("HouseworkTaskGenerateJob finished. insertedCount={}", inserted);
              return RepeatStatus.FINISHED;
            },
            transactionManager)
        .build();
  }
}
