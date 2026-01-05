package com.hwhub.batch;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HwHubBatchApplicationTests {

  @Disabled("CI does not have a database. Use unit tests instead.")
  @Test
  void contextLoads() {
  }
}
