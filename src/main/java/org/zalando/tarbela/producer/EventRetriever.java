package org.zalando.tarbela.producer;

public interface EventRetriever {
    EventsWithNextPage retrieveEvents();
}
