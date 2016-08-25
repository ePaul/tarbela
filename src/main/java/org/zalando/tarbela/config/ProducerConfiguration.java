package org.zalando.tarbela.config;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import org.springframework.web.client.RestOperations;

import org.zalando.stups.oauth2.spring.client.StupsOAuth2RestTemplate;
import org.zalando.stups.oauth2.spring.client.StupsTokensAccessTokenProvider;
import org.zalando.stups.tokens.AccessTokens;

import org.zalando.tarbela.config.util.ProducerConfigurations;
import org.zalando.tarbela.config.util.ProducerInteractor;
import org.zalando.tarbela.config.util.ProducerInteractorContainer;
import org.zalando.tarbela.producer.EventRetrieverImpl;
import org.zalando.tarbela.producer.EventStatusUpdaterImpl;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(ProducerConfigurations.class)
@Slf4j
public class ProducerConfiguration {
    private static final String QUERY_EVENT_STATUS_FILTER_NAME = "status";
    private static final String QUERY_EVENT_STATUS_FILTER_VALUE = "NEW";

    @Autowired
    private ProducerConfigurations producerConfigurations;

    @Autowired
    private HttpComponentsClientHttpRequestFactory requestFactory;

    @Bean
    public ProducerInteractorContainer producerInteractors() {

        final AccessTokens accessTokens = initializeAccessTokens();

        final List<ProducerInteractor> producerInteractors = new ArrayList<>();

        producerConfigurations.getProducers().forEach((producerName, producerProperties) ->
                producerInteractors.add(
                    new ProducerInteractor(
                        new EventRetrieverImpl(addEventFilterQueryParameter(producerProperties.getEventsUri()),
                            createTemplate(producerName, accessTokens)),
                        new EventStatusUpdaterImpl(producerProperties.getEventsUri(),
                            createTemplate(producerName, accessTokens)), producerProperties.getSchedulingInterval(),
                        producerName)));

        return new ProducerInteractorContainer(producerInteractors);
    }

    private RestOperations createTemplate(final String tokenName, final AccessTokens accessTokens) {
        return new StupsOAuth2RestTemplate(new StupsTokensAccessTokenProvider(tokenName, accessTokens), requestFactory);
    }

    private AccessTokens initializeAccessTokens() {
        final Map<String, List<String>> scopesMap = new HashMap<>();
        producerConfigurations.getProducers().forEach((producerName, producerProperties) ->
                scopesMap.put(producerName, producerProperties.getScopes()));
        return new AccessTokenProvider(scopesMap, producerConfigurations.getTokens().getCredentialsDirectory(),
                producerConfigurations.getTokens().getAccessTokenUri()).getAccessTokens();
    }

    //TODO: figure out how to test this
    private URI addEventFilterQueryParameter(final URI uri) {
        try {
            return new URIBuilder(uri).addParameter(QUERY_EVENT_STATUS_FILTER_NAME, QUERY_EVENT_STATUS_FILTER_VALUE)
                                      .build();
        } catch (URISyntaxException e) {
            log.error("Failed to construct producer URI with event filter from: " + uri);
            throw new IllegalStateException(e);
        }
    }
}
