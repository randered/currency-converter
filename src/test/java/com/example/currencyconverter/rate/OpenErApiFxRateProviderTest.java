package com.example.currencyconverter.rate;

import com.example.currencyconverter.common.ErrorCode;
import com.example.currencyconverter.config.FxProviderConfig;
import com.example.currencyconverter.exception.ApiException;
import com.example.currencyconverter.exception.ProviderUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenErApiFxRateProviderTest {

    private MockRestServiceServer server;
    private OpenErApiFxRateProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        FxProviderConfig props = new FxProviderConfig("http://provider.test", 1000, 1000);
        provider = new OpenErApiFxRateProvider(builder, props);
    }

    @AfterEach
    void verify() {
        server.verify();
    }

    @Test
    void fetchRateReadsRateForPair() {
        server.expect(requestTo("http://provider.test/latest/USD"))
                .andRespond(withSuccess("""
                        {"result":"success","base_code":"USD",
                         "rates":{"USD":1,"EUR":0.85,"GBP":0.75}}
                        """, MediaType.APPLICATION_JSON));

        BigDecimal rate = provider.fetchRate("USD", "EUR");

        assertThat(rate).isEqualByComparingTo("0.85");
    }

    @Test
    void fetchRateParsesRealProviderResponseShape() {
        // Mirrors the live payload from https://open.er-api.com/v6/latest/EUR,
        // including the metadata fields our record deliberately ignores.
        server.expect(requestTo("http://provider.test/latest/EUR"))
                .andRespond(withSuccess("""
                        {"result":"success","provider":"https://www.exchangerate-api.com",
                         "documentation":"https://www.exchangerate-api.com/docs/free",
                         "terms_of_use":"https://www.exchangerate-api.com/terms",
                         "time_last_update_unix":1787702551,
                         "time_last_update_utc":"Wed, 26 Aug 2026 00:02:31 +0000",
                         "time_next_update_unix":1787790701,
                         "time_next_update_utc":"Thu, 27 Aug 2026 00:31:41 +0000",
                         "time_eol_unix":0,
                         "base_code":"EUR",
                         "rates":{"EUR":1,"USD":1.166986,"GBP":0.855494,"AED":4.285756}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://provider.test/latest/EUR"))
                .andRespond(withSuccess("""
                        {"result":"success","provider":"https://www.exchangerate-api.com",
                         "base_code":"EUR",
                         "rates":{"EUR":1,"USD":1.166986,"GBP":0.855494,"AED":4.285756}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.fetchRate("EUR", "USD")).isEqualByComparingTo("1.166986");
        assertThat(provider.fetchRate("EUR", "GBP")).isEqualByComparingTo("0.855494");
    }

    @Test
    void fetchRateRejectsUnknownTargetCurrency() {
        server.expect(requestTo("http://provider.test/latest/USD"))
                .andRespond(withSuccess("""
                        {"result":"success","base_code":"USD","rates":{"USD":1}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.fetchRate("USD", "XXX"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.RATE_NOT_FOUND);
    }

    @Test
    void fetchRateRejectsUnsupportedBaseCurrency() {
        server.expect(requestTo("http://provider.test/latest/XXX"))
                .andRespond(withSuccess("""
                        {"result":"error","error-type":"unsupported-code","rates":{}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.fetchRate("XXX", "USD"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.RATE_NOT_FOUND);
    }

    @Test
    void fetchRateFailsGracefullyOnServerError() {
        server.expect(requestTo("http://provider.test/latest/USD"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.fetchRate("USD", "EUR"))
                .isInstanceOf(ProviderUnavailableException.class);
    }
}
