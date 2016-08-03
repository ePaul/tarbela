package org.zalando.tarbela.config.util;

import org.zalando.tarbela.producer.EventRetriever;
import org.zalando.tarbela.producer.EventStatusUpdater;

import lombok.Getter;

@Getter
public class ProducerInteractor {
    private final EventRetriever eventRetriever;

    private final EventStatusUpdater eventStatusUpdater;

    private final int jobInterval;

    public ProducerInteractor(final EventRetriever eventRetriever, final EventStatusUpdater eventStatusUpdater,
            final int jobInterval) {
        this.eventRetriever = eventRetriever;
        this.eventStatusUpdater = eventStatusUpdater;
        this.jobInterval = jobInterval;
    }
}
