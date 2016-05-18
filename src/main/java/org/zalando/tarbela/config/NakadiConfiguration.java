package org.zalando.tarbela.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import org.springframework.web.client.RestTemplate;

import org.zalando.stups.oauth2.spring.client.StupsOAuth2RestTemplate;
import org.zalando.stups.oauth2.spring.client.StupsTokensAccessTokenProvider;
import org.zalando.stups.tokens.AccessTokens;

import org.zalando.tarbela.nakadi.NakadiClient;
import org.zalando.tarbela.nakadi.NakadiClientImpl;

@Configuration
public class NakadiConfiguration {
    private static final String NAKADI_TOKEN_NAME = "zalando-nakadi";

    @Autowired
    private HttpComponentsClientHttpRequestFactory requestFactory;

    @Autowired
    private AccessTokens accessTokens;

    @Value("${nakadi.submission.uriTemplate}")
    private String nakadiURITemplate;

    @Bean
    public NakadiClient nakadiClient() {
        return new NakadiClientImpl(nakadiURITemplate, createTemplate(NAKADI_TOKEN_NAME));
    }

    private RestTemplate createTemplate(final String tokenName) {
        return new StupsOAuth2RestTemplate(new StupsTokensAccessTokenProvider(tokenName, accessTokens), requestFactory);
    }
}
