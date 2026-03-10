package com.juxa.legal_advice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class LegalAdviceApplication {

	public static void main(String[] args) {

        SpringApplication.run(LegalAdviceApplication.class, args);
	}

}
