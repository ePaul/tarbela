package org.zalando.tarbela.producer;

import java.util.List;

import org.zalando.tarbela.producer.models.EventUpdate;

/**
 * An object/service which is able to update event statuses to an event producer.
 */
public interface EventStatusUpdater {

    /**
     * Updates event statuses (in bulk form) to some producer.
     */
    void updateStatuses(List<? extends EventUpdate> updates);
}
