package org.zalando.tarbela.producer;

import java.net.URI;

import java.util.List;

import org.springframework.web.client.RestOperations;

import org.zalando.tarbela.producer.models.BunchOfEventUpdates;
import org.zalando.tarbela.producer.models.EventUpdate;

public class EventStatusUpdaterImpl implements EventStatusUpdater {

    private final RestOperations template;
    private final URI eventsUri;

    public EventStatusUpdaterImpl(final URI eventsUri, final RestOperations template) {
        this.template = template;
        this.eventsUri = eventsUri;
    }

    @Override
    public void updateStatuses(final List<EventUpdate> updates) {
        final BunchOfEventUpdates bunch = new BunchOfEventUpdates();
        bunch.setEvents(updates);

        template.postForEntity(eventsUri, bunch, Void.class);

        // TODO: exception handling here or where this is called?
    }

}
