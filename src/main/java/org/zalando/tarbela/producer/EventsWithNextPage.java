package org.zalando.tarbela.producer;

import java.util.List;
import java.util.Optional;

import org.zalando.tarbela.producer.models.Event;

public interface EventsWithNextPage {
    List<Event> getEvents();

    Optional<EventsWithNextPage> nextPage();
}
