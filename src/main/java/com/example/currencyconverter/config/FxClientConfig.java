package com.example.currencyconverter.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class FxClientConfig {

    @Bean
    public RestClientCustomizer fxRestClientCustomizer(FxProviderConfig props) {
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(props.connectTimeoutMs());
            factory.setReadTimeout(props.readTimeoutMs());
            builder.requestFactory(factory);
        };
    }
}
