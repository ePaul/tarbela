package org.zalando.tarbela.config.util;

import org.zalando.tarbela.producer.EventRetriever;
import org.zalando.tarbela.producer.EventStatusUpdater;

import lombok.Getter;

/**
 * This class contains all information needed by the {@link EventService} to interact with a producer.
 */
@Getter
public class ProducerInteractor {
    private final EventRetriever eventRetriever;

    private final EventStatusUpdater eventStatusUpdater;

    private final int jobInterval;
    private final String producerName;

    public ProducerInteractor(final EventRetriever eventRetriever, final EventStatusUpdater eventStatusUpdater,
            final int jobInterval, final String producerName) {
        this.eventRetriever = eventRetriever;
        this.eventStatusUpdater = eventStatusUpdater;
        this.jobInterval = jobInterval;
        this.producerName = producerName;
    }
}
