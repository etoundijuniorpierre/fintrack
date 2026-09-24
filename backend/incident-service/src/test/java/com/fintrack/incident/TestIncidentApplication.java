package com.fintrack.incident;

import org.springframework.boot.SpringApplication;

public class TestIncidentApplication {

  public static void main(String[] args) {
    SpringApplication.from(IncidentApplication::main)
      .with(TestcontainersConfiguration.class)
      .run(args);
  }
}
