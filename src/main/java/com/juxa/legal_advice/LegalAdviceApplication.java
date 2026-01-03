package com.juxa.legal_advice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
public class LegalAdviceApplication {

	public static void main(String[] args) {

        SpringApplication.run(LegalAdviceApplication.class, args);
	}

}
