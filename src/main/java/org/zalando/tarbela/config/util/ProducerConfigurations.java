package org.zalando.tarbela.config.util;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties
@Getter
@Setter
public class ProducerConfigurations {
    private Map<String, ProducerConfigurationDetails> producers;

    private TokenInformation tokens;

    @Getter
    @Setter
    public static class TokenInformation {
        private String accessTokenUri;
        private String tokenInfoUri;
        private String credentialsDirectory;
    }
}
