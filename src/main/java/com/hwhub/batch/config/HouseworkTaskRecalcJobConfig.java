package com.hwhub.batch.config;

import com.hwhub.batch.application.service.HouseworkTaskRecalcService;
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
public class HouseworkTaskRecalcJobConfig {

  public static final String JOB_NAME = "houseworkTaskRecalcJob";
  public static final String STEP_NAME = "houseworkTaskRecalcStep";

  // 1回のジョブで処理するキューの最大件数（必要に応じて調整）
  private static final int FETCH_LIMIT = 100;

  @NonNull private final JobRepository jobRepository;
  @NonNull private final PlatformTransactionManager transactionManager;
  @NonNull private final HouseworkTaskRecalcService service;

  @Bean
  public Job houseworkTaskRecalcJob() {
    return new JobBuilder(JOB_NAME, jobRepository)
        .start(Objects.requireNonNull(houseworkTaskRecalcStep()))
        .build();
  }

  @Bean
  public Step houseworkTaskRecalcStep() {
    return new StepBuilder(STEP_NAME, jobRepository)
        .tasklet(
            (contribution, chunkContext) -> {
              int affected = service.recalcPending(FETCH_LIMIT);
              log.info("HouseworkTaskRecalcJob finished. affectedCount={}", affected);
              return RepeatStatus.FINISHED;
            },
            transactionManager)
        .build();
  }
}
