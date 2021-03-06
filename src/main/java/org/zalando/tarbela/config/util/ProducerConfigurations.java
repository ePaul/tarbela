package org.zalando.tarbela.config.util;

import java.net.URI;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * This class will be initialized and filled by Spring using the details of the Spring application configuration.
 */
@ConfigurationProperties
@Getter
@Setter
public class ProducerConfigurations {
    private Map<String, ProducerProperties> producers;

    private TokenInformation tokens;

    @Getter
    @Setter
    public static class ProducerProperties {
        private List<String> scopes;
        private int schedulingInterval;
        private URI eventsUri;
    }

    @Getter
    @Setter
    public static class TokenInformation {
        private String accessTokenUri;
        private String tokenInfoUri;
        private String credentialsDirectory;
    }
}
