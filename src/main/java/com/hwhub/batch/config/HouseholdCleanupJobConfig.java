package com.hwhub.batch.config;

import com.hwhub.batch.application.service.HouseholdCleanupService;
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
public class HouseholdCleanupJobConfig {

    public static final String JOB_NAME = "householdCleanupJob";
    public static final String STEP_NAME = "householdCleanupStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final HouseholdCleanupService service;

    @Bean
    public Job householdCleanupJob() {
        return new JobBuilder(JOB_NAME, jobRepository).start(householdCleanupStep()).build();
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
