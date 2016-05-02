package org.zalando.tarbela.config;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.client.RestTemplate;

import org.zalando.stups.tokens.annotation.OAuth2RestOperationsAutowired;

import org.zalando.tarbela.nakadi.NakadiClient;
import org.zalando.tarbela.nakadi.NakadiClientImpl;

@Configuration
public class NakadiConfiguration {
    private static final String NAKADI_TOKEN_NAME = "zalando-nakadi";

    @OAuth2RestOperationsAutowired(NAKADI_TOKEN_NAME)
    private RestTemplate template;

    @Value("${NAKADI_SUBMISSION_URI}")
    private String nakadiURI;

    @Bean
    public NakadiClient nakadiClient() {
        return new NakadiClientImpl(nakadiURI, template);
    }
}
