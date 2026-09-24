package com.fintrack.reporting;

import org.springframework.boot.SpringApplication;

public class TestReportingApplication {

  public static void main(String[] args) {
    SpringApplication.from(ReportingApplication::main)
      .with(TestcontainersConfiguration.class)
      .run(args);
  }
}
