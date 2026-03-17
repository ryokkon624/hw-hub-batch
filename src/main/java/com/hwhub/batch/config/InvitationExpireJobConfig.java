package com.hwhub.batch.config;

import com.hwhub.batch.application.service.InvitationExpireService;
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
public class InvitationExpireJobConfig {

  public static final String JOB_NAME = "invitationExpireJob";
  public static final String STEP_NAME = "invitationExpireStep";

  @NonNull private final JobRepository jobRepository;
  @NonNull private final PlatformTransactionManager transactionManager;
  @NonNull private final InvitationExpireService service;

  @Bean
  public Job invitationExpireJob() {
    return new JobBuilder(JOB_NAME, jobRepository)
        .start(Objects.requireNonNull(invitationExpireStep()))
        .build();
  }

  @Bean
  public Step invitationExpireStep() {
    return new StepBuilder(STEP_NAME, jobRepository)
        .tasklet(
            (contribution, chunkContext) -> {
              int updated = service.expireInvitations();
              log.info("InvitationExpireJob finished. updatedCount={}", updated);
              return RepeatStatus.FINISHED;
            },
            transactionManager)
        .build();
  }
}
