package org.zalando.tarbela.config.util;

import java.net.URI;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProducerConfigurationDetails {
    private List<String> scopes;
    private int schedulingInterval;
    private URI eventsUri;
}
