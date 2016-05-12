package org.zalando.tarbela.config;

import java.net.URI;

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
import org.zalando.tarbela.producer.EventStatusUpdater;
import org.zalando.tarbela.producer.EventStatusUpdaterImpl;

@Configuration
public class ProducerConfiguration {

    private static final String PRODUCER_TOKEN_NAME = "producer";

    @Value("producer.event.uri")
    private URI producerEventsURI;

    @Autowired
    private HttpComponentsClientHttpRequestFactory requestFactory;

    @Autowired
    private AccessTokens accessTokens;

    @Bean
    public EventRetriever eventRetriever() {
        return new EventRetrieverImpl(producerEventsURI, createTemplate(PRODUCER_TOKEN_NAME));
    }

    @Bean
    public EventStatusUpdater eventUpdater() {
        return new EventStatusUpdaterImpl(createTemplate(PRODUCER_TOKEN_NAME), producerEventsURI);
    }

    private RestOperations createTemplate(final String tokenName) {
        return new StupsOAuth2RestTemplate(new StupsTokensAccessTokenProvider(tokenName, accessTokens), requestFactory);
    }

}
