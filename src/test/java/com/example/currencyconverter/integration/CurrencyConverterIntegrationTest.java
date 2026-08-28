package com.example.currencyconverter.integration;

import com.example.currencyconverter.dto.BalanceDto;
import com.example.currencyconverter.dto.ConversionResponse;
import com.example.currencyconverter.rate.FxRateProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test through the Spring context against a real Postgres started
 * with Testcontainers. The external FX provider is stubbed so tests never hit
 * the network. @Transactional makes every test method roll back its changes,
 * keeping the seeded demo balances consistent between tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class CurrencyConverterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FxRateProvider fxRateProvider;

    private String convert(String clientId, String idempotencyKey, String body) throws Exception {
        var req = post("/conversions")
                .header("X-Client-Id", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (idempotencyKey != null) {
            req.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(req).andReturn().getResponse().getContentAsString();
    }

    private String body(String from, String to, String amount) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "sourceCurrency", from,
                "targetCurrency", to,
                "amount", new BigDecimal(amount)));
    }

    private List<BalanceDto> balances(String clientId) throws Exception {
        String json = mockMvc.perform(get("/clients/" + clientId + "/balances"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    private BigDecimal balanceOf(String clientId, String currency) throws Exception {
        return balances(clientId).stream()
                .filter(b -> b.currency().equals(currency))
                .findFirst().orElseThrow()
                .amount();
    }

    private int historyCount(String... params) throws Exception {
        MockHttpServletRequestBuilder req = get("/conversions");
        for (int i = 0; i < params.length; i += 2) {
            req.param(params[i], params[i + 1]);
        }
        String json = mockMvc.perform(req).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).path("page").path("totalElements").asInt();
    }

    @Test
    void happyPathConversionDebitsCreditsAndIsStored() throws Exception {
        when(fxRateProvider.fetchRate("USD", "EUR")).thenReturn(new BigDecimal("0.85"));

        ConversionResponse response = objectMapper.readValue(
                convert("CLIENT-001", null, body("USD", "EUR", "100.00")),
                ConversionResponse.class);

        assertThat(response.transactionId()).isNotNull();
        assertThat(response.sourceCurrency()).isEqualTo("USD");
        assertThat(response.targetCurrency()).isEqualTo("EUR");
        assertThat(response.sourceAmount()).isEqualByComparingTo("100.00");
        assertThat(response.targetAmount()).isEqualByComparingTo("85.00");
        assertThat(response.rate()).isEqualByComparingTo("0.85");

        // Updated balances: 10,000 - 100 = 9,900 USD; 8,000 + 85 = 8,085 EUR.
        assertThat(balanceOf("CLIENT-001", "USD")).isEqualByComparingTo("9900.00");
        assertThat(balanceOf("CLIENT-001", "EUR")).isEqualByComparingTo("8085.00");

        // History by client and by transactionId both contain exactly one record.
        assertThat(historyCount("clientId", "CLIENT-001")).isEqualTo(1);
        assertThat(historyCount("transactionId", response.transactionId().toString())).isEqualTo(1);
    }

    @Test
    void insufficientFundsReturns422AndDoesNotPersist() throws Exception {
        when(fxRateProvider.fetchRate("USD", "EUR")).thenReturn(new BigDecimal("0.85"));

        mockMvc.perform(post("/conversions")
                        .header("X-Client-Id", "CLIENT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("USD", "EUR", "999999.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        // Balances untouched and no conversion record stored.
        assertThat(balanceOf("CLIENT-001", "USD")).isEqualByComparingTo("10000.00");
        assertThat(historyCount("clientId", "CLIENT-001")).isZero();
    }

    @Test
    void idempotencyReplayReturnsOriginalAndDoesNotDoubleDebit() throws Exception {
        when(fxRateProvider.fetchRate("USD", "EUR")).thenReturn(new BigDecimal("0.85"));

        ConversionResponse first = objectMapper.readValue(
                convert("CLIENT-001", "key-1", body("USD", "EUR", "100.00")),
                ConversionResponse.class);
        ConversionResponse second = objectMapper.readValue(
                convert("CLIENT-001", "key-1", body("USD", "EUR", "100.00")),
                ConversionResponse.class);

        assertThat(second.transactionId()).isEqualTo(first.transactionId());
        assertThat(second.targetAmount()).isEqualByComparingTo(first.targetAmount());

        // Debited exactly once: 10,000 - 100 = 9,900.
        assertThat(balanceOf("CLIENT-001", "USD")).isEqualByComparingTo("9900.00");
        assertThat(historyCount("clientId", "CLIENT-001")).isEqualTo(1);
    }

    @Test
    void unknownClientReturns404() throws Exception {
        mockMvc.perform(post("/conversions")
                        .header("X-Client-Id", "NOBODY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("USD", "EUR", "10.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
    }

    @Test
    void sourceCurrencyNotHeldReturns404BalanceNotFound() throws Exception {
        when(fxRateProvider.fetchRate("GBP", "USD")).thenReturn(new BigDecimal("1.30"));

        // CLIENT-002 only holds GBP, so converting from USD must fail.
        mockMvc.perform(post("/conversions")
                        .header("X-Client-Id", "CLIENT-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("USD", "GBP", "10.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BALANCE_NOT_FOUND"));
    }

    @Test
    void historyRequiresAtLeastOneFilter() throws Exception {
        mockMvc.perform(get("/conversions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void ratesEndpointReturnsRate() throws Exception {
        when(fxRateProvider.fetchRate("USD", "EUR")).thenReturn(new BigDecimal("0.85"));

        mockMvc.perform(get("/rates").param("from", "USD").param("to", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(0.85))
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"));
    }

    @Test
    void conversionToNewCurrencyCreatesBalanceRow() throws Exception {
        when(fxRateProvider.fetchRate("EUR", "GBP")).thenReturn(new BigDecimal("0.85"));

        // CLIENT-001 holds USD + EUR but not GBP; converting EUR -> GBP credits a new row.
        ConversionResponse response = objectMapper.readValue(
                convert("CLIENT-001", null, body("EUR", "GBP", "100.00")),
                ConversionResponse.class);

        assertThat(response.balances()).extracting(BalanceDto::currency)
                .contains("GBP");
        assertThat(balanceOf("CLIENT-001", "GBP")).isEqualByComparingTo("85.00");
        assertThat(balanceOf("CLIENT-001", "EUR")).isEqualByComparingTo("7900.00");
    }
}
