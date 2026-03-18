package com.hwhub.batch.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ManualJobRunnerConfig {

  private static final Logger log = LoggerFactory.getLogger(ManualJobRunnerConfig.class);

  @Bean
  public ApplicationRunner manualJobRunner(JobOperator jobOperator, List<Job> jobs) {
    return new ApplicationRunner() {
      @Override
      public void run(ApplicationArguments args) throws Exception {

        // --- 起動引数から job 名を取得 ---
        List<String> optionValues = args.getOptionValues("spring.batch.job.name");
        if (optionValues == null || optionValues.isEmpty()) {
          log.info(
              "No 'spring.batch.job.name' specified. ManualJobRunner will not launch any job.(e.g. --spring.batch.job.name=invitationExpireJob)");
          return;
        }

        String jobName = optionValues.getFirst();
        Job job =
            jobs.stream()
                .filter(j -> j.getName().equals(jobName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobName));

        log.info(">>> Launching job '{}'", jobName);

        // --- 起動 ---
        JobParameters params =
            new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        jobOperator.run(job, params);

        log.info(">>> Job '{}' finished.", jobName);
      }
    };
  }
}
