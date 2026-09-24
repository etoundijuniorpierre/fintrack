package com.fintrack.audit;

import org.springframework.boot.SpringApplication;

public class TestAuditApplication {

  public static void main(String[] args) {
    SpringApplication.from(AuditApplication::main)
      .with(TestcontainersConfiguration.class)
      .run(args);
  }
}
