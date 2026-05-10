package com.crewmeister.cmcodingchallenge;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

	@Test
	void should_return_exact_rate_when_requested_day_exists() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.AUD.A", "AUD", LocalDate.parse("2026-05-01"), 1.6432));

		mockMvc.perform(get("/api/rates/AUD").param("date", "2026-05-01"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"currency":"AUD","requestedDate":"2026-05-01","effectiveRateDate":"2026-05-01","rate":1.6432}
						"""));
	}

	@Test
	void should_return_previous_business_day_rate_when_requested_day_has_no_rate() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.AUD.A", "AUD", LocalDate.parse("2026-04-30"), 1.6393));
		fxRateRepository.save(new FxRateEntity("D.AUD.A", "AUD", LocalDate.parse("2026-05-02"), 1.6500));

		mockMvc.perform(get("/api/rates/AUD").param("date", "2026-05-01"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"currency":"AUD","requestedDate":"2026-05-01","effectiveRateDate":"2026-04-30","rate":1.6393}
						"""));
	}

	@Test
	void should_return_not_found_when_no_rate_exists_on_or_before_requested_day() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.AUD.A", "AUD", LocalDate.parse("2026-05-02"), 1.6500));

		mockMvc.perform(get("/api/rates/AUD").param("date", "2026-05-01"))
				.andExpect(status().isNotFound());
	}

}
