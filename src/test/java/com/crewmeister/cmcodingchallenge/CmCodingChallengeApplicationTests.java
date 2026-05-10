package com.crewmeister.cmcodingchallenge;

import com.crewmeister.cmcodingchallenge.currency.service.CurrencyQueryService;
import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CmCodingChallengeApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CacheManager cacheManager;

	@SpyBean
	private FxRateRepository fxRateRepository;

	@BeforeEach
	void cleanStore() {
		fxRateRepository.deleteAll();
		if (cacheManager.getCache(CurrencyQueryService.AVAILABLE_CURRENCIES_CACHE) != null) {
			cacheManager.getCache(CurrencyQueryService.AVAILABLE_CURRENCIES_CACHE).clear();
		}
	}

	@Test
	void contextLoads() {
	}

	@Test
	void given_empty_store_when_getting_currencies_then_return_empty_list() throws Exception {
		mockMvc.perform(get("/api/currencies")).andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	void given_store_with_rates_when_getting_currencies_then_return_sorted_distinct_list() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.USD.A", "USD", LocalDate.parse("2024-01-01"), 1.1));
		fxRateRepository.save(new FxRateEntity("D.USD.A", "USD", LocalDate.parse("2024-01-02"), 1.2));
		fxRateRepository.save(new FxRateEntity("D.CHF.A", "CHF", LocalDate.parse("2024-01-01"), 0.9));

		mockMvc.perform(get("/api/currencies"))
				.andExpect(status().isOk())
				.andExpect(content().json("[\"CHF\",\"USD\"]"));
	}

	@Test
	void given_cached_currency_lookup_when_called_multiple_times_then_query_repository_once() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.USD.A", "USD", LocalDate.parse("2024-01-01"), 1.1));
		clearInvocations(fxRateRepository);

		mockMvc.perform(get("/api/currencies"))
				.andExpect(status().isOk())
				.andExpect(content().json("[\"USD\"]"));
		mockMvc.perform(get("/api/currencies"))
				.andExpect(status().isOk())
				.andExpect(content().json("[\"USD\"]"));

		verify(fxRateRepository, times(1)).findDistinctCurrencies();
	}

	@Test
	void given_existing_rate_for_requested_day_when_getting_rate_then_return_exact_rate() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.AUD.A", "AUD", LocalDate.parse("2026-05-01"), 1.6432));

		mockMvc.perform(get("/api/rates/AUD").param("date", "2026-05-01"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"currency":"AUD","requestedDate":"2026-05-01","effectiveRateDate":"2026-05-01","rate":1.6432}
						"""));
	}

	@Test
	void given_missing_requested_day_rate_when_getting_rate_then_return_previous_business_day_rate() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.AUD.A", "AUD", LocalDate.parse("2026-04-30"), 1.6393));
		fxRateRepository.save(new FxRateEntity("D.AUD.A", "AUD", LocalDate.parse("2026-05-02"), 1.6500));

		mockMvc.perform(get("/api/rates/AUD").param("date", "2026-05-01"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"currency":"AUD","requestedDate":"2026-05-01","effectiveRateDate":"2026-04-30","rate":1.6393}
						"""));
	}

	@Test
	void given_no_rate_on_or_before_requested_day_when_getting_rate_then_return_not_found() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.AUD.A", "AUD", LocalDate.parse("2026-05-02"), 1.6500));

		mockMvc.perform(get("/api/rates/AUD").param("date", "2026-05-01"))
				.andExpect(status().isNotFound());
	}

	@Test
	void given_currency_rate_history_when_getting_history_page_then_return_sorted_page() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.AUD.A", "AUD", LocalDate.parse("2026-05-02"), 1.6500));
		fxRateRepository.save(new FxRateEntity("D.AUD.A", "AUD", LocalDate.parse("2026-05-01"), 1.6432));
		fxRateRepository.save(new FxRateEntity("D.AUD.A", "AUD", LocalDate.parse("2026-05-03"), 1.6550));

		mockMvc.perform(get("/api/rates/AUD/history").param("page", "0").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"content":[
						 {"currency":"AUD","requestedDate":"2026-05-01","effectiveRateDate":"2026-05-01","rate":1.6432},
						 {"currency":"AUD","requestedDate":"2026-05-02","effectiveRateDate":"2026-05-02","rate":1.6500}
						],"number":0,"size":2,"totalElements":3,"totalPages":2}
						"""));
	}

	@Test
	void given_currency_without_history_when_getting_history_then_return_not_found() throws Exception {
		mockMvc.perform(get("/api/rates/AUD/history"))
				.andExpect(status().isNotFound());
	}

	@Test
	void given_invalid_history_page_request_when_getting_history_then_return_bad_request() throws Exception {
		mockMvc.perform(get("/api/rates/AUD/history").param("page", "-1").param("size", "0"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void given_exact_rate_on_date_when_converting_to_eur_then_return_converted_amount() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.USD.A", "USD", LocalDate.parse("2026-05-01"), 1.1));

		mockMvc.perform(get("/api/rates/convert")
				.param("currency", "USD")
				.param("amount", "100")
				.param("date", "2026-05-01"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"sourceCurrency":"USD","sourceAmount":100.0,"conversionDate":"2026-05-01","eurEquivalent":90.90909090909091}
						"""));
	}

	@Test
	void given_missing_requested_day_rate_when_converting_to_eur_then_use_previous_business_day_rate() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.USD.A", "USD", LocalDate.parse("2026-04-30"), 1.05));
		fxRateRepository.save(new FxRateEntity("D.USD.A", "USD", LocalDate.parse("2026-05-02"), 1.12));

		mockMvc.perform(get("/api/rates/convert")
				.param("currency", "USD")
				.param("amount", "100")
				.param("date", "2026-05-01"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"sourceCurrency":"USD","sourceAmount":100.0,"conversionDate":"2026-05-01","eurEquivalent":95.23809523809524}
						"""));
	}

	@Test
	void given_no_available_rate_when_converting_to_eur_then_return_not_found() throws Exception {
		fxRateRepository.save(new FxRateEntity("D.USD.A", "USD", LocalDate.parse("2026-05-02"), 1.1));

		mockMvc.perform(get("/api/rates/convert")
				.param("currency", "USD")
				.param("amount", "100")
				.param("date", "2026-05-01"))
				.andExpect(status().isNotFound());
	}

}
