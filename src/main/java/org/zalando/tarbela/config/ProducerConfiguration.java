package org.zalando.tarbela.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestOperations;
import org.zalando.stups.tokens.annotation.OAuth2RestOperationsAutowired;
import org.zalando.tarbela.producer.EventRetriever;
import org.zalando.tarbela.producer.EventRetrieverImpl;

@Configuration
public class ProducerConfiguration {

    private static final String PRODUCER_TOKEN_NAME = "producer";

    @Value("${PRODUCER_EVENTS_URI}")
    private String producerEventsURI;

    @OAuth2RestOperationsAutowired(PRODUCER_TOKEN_NAME)
    private RestOperations template;

    @Bean
    public EventRetriever getEventRetriever() {
        return new EventRetrieverImpl(producerEventsURI, template);
    }

}
