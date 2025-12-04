package com.hwhub.batch.config;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ManualJobRunnerConfig {

  private static final Logger log = LoggerFactory.getLogger(ManualJobRunnerConfig.class);

  @Bean
  public ApplicationRunner manualJobRunner(JobLauncher jobLauncher, List<Job> jobs) {
    return new ApplicationRunner() {
      @Override
      public void run(ApplicationArguments args) throws Exception {

        // --- 起動引数から job 名を取得 ---
        String jobName =
            Optional.ofNullable(args.getOptionValues("spring.batch.job.name"))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "spring.batch.job.name is required. (e.g. --spring.batch.job.name=invitationExpireJob)"));

        // --- 登録されている Job の中から該当 Job を探す ---
        Job job =
            jobs.stream()
                .filter(j -> j.getName().equals(jobName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobName));

        // --- JobParameters ---
        JobParameters params =
            new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        log.info(">>> Launching job '{}' with params={}", jobName, params);

        // --- 起動 ---
        jobLauncher.run(job, params);

        log.info(">>> Job '{}' finished.", jobName);
      }
    };
  }
}
