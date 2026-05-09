package com.crewmeister.cmcodingchallenge;

import com.crewmeister.cmcodingchallenge.currency.CurrencyQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CmCodingChallengeApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(CmCodingChallengeApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(CmCodingChallengeApplication.class, args);
	}

	@Bean
	ApplicationRunner currencyBootstrapRunner(CurrencyQueryService currencyQueryService) {
		return args -> {
			currencyQueryService.getAvailableCurrencies();
			LOGGER.info("Startup currency bootstrap triggered");
		};
	}

}
