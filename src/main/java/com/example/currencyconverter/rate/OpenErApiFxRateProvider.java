package com.example.currencyconverter.rate;

import com.example.currencyconverter.common.ErrorCode;
import com.example.currencyconverter.config.FxProviderConfig;
import com.example.currencyconverter.dto.OpenErRateResponse;
import com.example.currencyconverter.exception.NotFoundException;
import com.example.currencyconverter.exception.ProviderUnavailableException;
import com.example.currencyconverter.util.Constants;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class OpenErApiFxRateProvider implements FxRateProvider {

    private final RestClient restClient;

    public OpenErApiFxRateProvider(RestClient.Builder builder, FxProviderConfig props) {
        this.restClient = builder
                .baseUrl(props.baseUrl())
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw new ProviderUnavailableException(
                            Constants.FX_PROVIDER_HTTP_ERROR_PREFIX + response.getStatusCode().value());
                })
                .build();
    }

    @Override
    public BigDecimal fetchRate(String from, String to) {
        OpenErRateResponse body;
        try {
            body = restClient.get()
                    .uri(Constants.FX_PROVIDER_LATEST_PATH, from)
                    .retrieve()
                    .body(OpenErRateResponse.class);
        } catch (ProviderUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderUnavailableException(Constants.FX_PROVIDER_UNREACHABLE, e);
        }

        if (body == null || body.rates() == null) {
            throw new ProviderUnavailableException(Constants.FX_PROVIDER_EMPTY_RESPONSE);
        }
        if (!Constants.FX_PROVIDER_RESULT_SUCCESS.equals(body.result())) {
            throw new NotFoundException(ErrorCode.RATE_NOT_FOUND, Constants.RATE_NOT_FOUND.formatted(from));
        }

        BigDecimal rate = body.rates().get(to);
        if (rate == null) {
            throw new NotFoundException(ErrorCode.RATE_NOT_FOUND, Constants.RATE_NOT_FOUND.formatted(to));
        }
        return rate;
    }
}
