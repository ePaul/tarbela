package org.zalando.tarbela.producer;

/**
 * An abstraction of a service from which events can be retrieved.
 */
public interface EventRetriever {

    /**
     * Retrieves a page of events. That page itself possibly allows to fetch more pages.
     *
     * @return  an EventsWithNextPage object, containing one page of events and a way to get more.
     */
    EventsWithNextPage retrieveEvents();
}
