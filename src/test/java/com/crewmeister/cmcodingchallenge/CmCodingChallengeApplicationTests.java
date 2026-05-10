package com.crewmeister.cmcodingchallenge;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CmCodingChallengeApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FxRateRepository fxRateRepository;

	@BeforeEach
	void cleanStore() {
		fxRateRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void should_return_empty_currency_list_when_store_is_empty() throws Exception {
		mockMvc.perform(get("/api/currencies")).andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	void should_return_sorted_distinct_currency_list_when_store_has_rates() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.USD.A", "USD", LocalDate.parse("2024-01-01"), 1.1));
		fxRateRepository.save(new FxRateEntity("D.USD.A", "USD", LocalDate.parse("2024-01-02"), 1.2));
		fxRateRepository.save(new FxRateEntity("D.CHF.A", "CHF", LocalDate.parse("2024-01-01"), 0.9));

		mockMvc.perform(get("/api/currencies"))
				.andExpect(status().isOk())
				.andExpect(content().json("[\"CHF\",\"USD\"]"));
	}

}
