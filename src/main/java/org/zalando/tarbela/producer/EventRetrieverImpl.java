package org.zalando.tarbela.producer;

import static org.zalando.tarbela.util.StringConstants.CONTENT_TYPE_BUNCH_OF_EVENTS;
import static org.zalando.tarbela.util.StringConstants.CONTENT_TYPE_PROBLEM_JSON;

import java.net.URI;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.client.RestOperations;

import org.zalando.tarbela.producer.models.BunchOfEvents;
import org.zalando.tarbela.producer.models.BunchofEventsLinks;
import org.zalando.tarbela.producer.models.BunchofEventsLinksNext;
import org.zalando.tarbela.producer.models.Event;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventRetrieverImpl implements EventRetriever {

    private static final MediaType MEDIA_TYPE_PROBLEM = MediaType.parseMediaType(CONTENT_TYPE_PROBLEM_JSON);
    private static final MediaType MEDIA_TYPE_EVENT_LIST = MediaType.parseMediaType(CONTENT_TYPE_BUNCH_OF_EVENTS);

    private final URI retrievalUrl;
    private final RestOperations restTemplate;

    public EventRetrieverImpl(final URI retrievalUrl, final RestOperations restTemplate) {
        log.info("EventRetriever is using {}", retrievalUrl);
        this.retrievalUrl = retrievalUrl;
        this.restTemplate = restTemplate;
    }

    private static class EventsWithNextPageImpl implements EventsWithNextPage {
        private List<Event> events;
        private Optional<EventRetriever> nextRetriever;

        private EventsWithNextPageImpl(final List<Event> events, final Optional<EventRetriever> nextRetriever) {
            this.events = events;
            this.nextRetriever = nextRetriever;
        }

        @Override
        public List<Event> getEvents() {
            return events;
        }

        @Override
        public Optional<EventsWithNextPage> nextPage() {
            if (nextRetriever.isPresent()) {
                return Optional.of(nextRetriever.get().retrieveEvents());
            } else {
                return Optional.empty();
            }
        }

    }

    @Override
    public EventsWithNextPage retrieveEvents() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MEDIA_TYPE_PROBLEM, MEDIA_TYPE_EVENT_LIST));

        final HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        final ResponseEntity<BunchOfEvents> response = restTemplate.exchange(retrievalUrl, HttpMethod.GET,
                requestEntity, BunchOfEvents.class);
        final BunchOfEvents bunch = response.getBody();
        return new EventsWithNextPageImpl(bunch.getEvents(), nextRetriever(bunch));
            // TODO: do we need to handle exceptions here?
    }

    private Optional<EventRetriever> nextRetriever(final BunchOfEvents bunch) {
        final BunchofEventsLinks links = bunch.getLinks();
        if (links != null) {
            final BunchofEventsLinksNext nextLink = links.getNext();
            if (nextLink != null) {
                final String nextLinkHref = nextLink.getHref();
                if (nextLinkHref != null) {
                    return Optional.of(new EventRetrieverImpl(retrievalUrl.resolve(nextLinkHref), restTemplate));
                }
            }
        }

        return Optional.empty();
    }

}
