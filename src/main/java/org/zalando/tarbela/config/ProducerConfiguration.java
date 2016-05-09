package org.zalando.tarbela.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import org.springframework.web.client.RestOperations;

import org.zalando.stups.oauth2.spring.client.StupsOAuth2RestTemplate;
import org.zalando.stups.oauth2.spring.client.StupsTokensAccessTokenProvider;
import org.zalando.stups.tokens.AccessTokens;

import org.zalando.tarbela.producer.EventRetriever;
import org.zalando.tarbela.producer.EventRetrieverImpl;

@Configuration
public class ProducerConfiguration {

    private static final String PRODUCER_TOKEN_NAME = "producer";

    @Value("${PRODUCER_EVENTS_URI}")
    private String producerEventsURI;

    @Autowired
    private HttpComponentsClientHttpRequestFactory requestFactory;

    @Autowired
    private AccessTokens accessTokens;

    @Bean
    public EventRetriever getEventRetriever() {
        return new EventRetrieverImpl(producerEventsURI, createTemplate(PRODUCER_TOKEN_NAME));
    }

    private RestOperations createTemplate(final String tokenName) {
        return new StupsOAuth2RestTemplate(new StupsTokensAccessTokenProvider(tokenName, accessTokens), requestFactory);
    }

}
