package com.hwhub.batch.config;

import com.hwhub.batch.application.service.HouseholdCleanupService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class HouseholdCleanupJobConfig {

  public static final String JOB_NAME = "householdCleanupJob";
  public static final String STEP_NAME = "householdCleanupStep";

  @NonNull private final JobRepository jobRepository;
  @NonNull private final PlatformTransactionManager transactionManager;
  @NonNull private final HouseholdCleanupService service;

  @Bean
  public Job householdCleanupJob() {
    return new JobBuilder(JOB_NAME, jobRepository)
        .start(Objects.requireNonNull(householdCleanupStep()))
        .build();
  }

  @Bean
  public Step householdCleanupStep() {
    return new StepBuilder(STEP_NAME, jobRepository)
        .tasklet(
            (contribution, chunkContext) -> {
              service.cleanupHouseholds();
              return RepeatStatus.FINISHED;
            },
            transactionManager)
        .build();
  }
}
