package org.zalando.tarbela.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.zalando.tarbela.producer.EventRetrieverImpl;
import org.zalando.tarbela.producer.EventStatusUpdaterImpl;

@Configuration
@EnableConfigurationProperties(ProducerConfigurations.ProducerProperties.class)
public class ProducerConfiguration {

    @Autowired
    private ProducerConfigurations producerConfigurations;

    @Autowired
    private HttpComponentsClientHttpRequestFactory requestFactory;

    private AccessTokens accessTokens;

    private AccessTokenProvider accessTokenProvider;

    @Bean
    public List<ProducerInteractor> producerInteractors() {
        if (accessTokens == null) {
            initializeAccessTokens();
        }

        final List<ProducerInteractor> producerInteractors = new ArrayList<>();

        producerConfigurations.getProducers().forEach((producerName, producerProperties) ->
                producerInteractors.add(
                    new ProducerInteractor(
                        new EventRetrieverImpl(producerProperties.getEventsUri(), createTemplate(producerName)),
                        new EventStatusUpdaterImpl(producerProperties.getEventsUri(), createTemplate(producerName)),
                        producerProperties.getSchedulingInterval())));

        return producerInteractors;
    }

    private RestOperations createTemplate(final String tokenName) {
        return new StupsOAuth2RestTemplate(new StupsTokensAccessTokenProvider(tokenName, accessTokens), requestFactory);
    }

    private void initializeAccessTokens() {
        final Map<String, List<String>> scopesMap = new HashMap<>();
        producerConfigurations.getProducers().forEach((producerName, producerProperties) ->
                scopesMap.put(producerName, producerProperties.getScopes()));
        accessTokens = new AccessTokenProvider(scopesMap, producerConfigurations.getTokens().getCredentialsDirectory(),
                producerConfigurations.getTokens().getAccessTokenUri()).getAccessTokens();
    }
}
