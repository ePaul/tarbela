package org.zalando.tarbela.producer;

import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.client.RestOperations;

import org.zalando.tarbela.producer.models.BunchOfEvents;
import org.zalando.tarbela.producer.models.BunchofEventsLinks;
import org.zalando.tarbela.producer.models.BunchofEventsLinksNext;
import org.zalando.tarbela.producer.models.Event;

import com.google.common.collect.ImmutableList;

public class EventRetrieverTest {

    private static final String PRODUCER_EVENTS_URL = "https://example.org/events";

    @Mock
    private RestOperations template;
    @Mock
    private BunchOfEvents bunch;

    private EventRetriever retriever;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(new ResponseEntity<>(bunch, HttpStatus.OK)) //
        .when(template).exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(BunchOfEvents.class));

        retriever = new EventRetrieverImpl(PRODUCER_EVENTS_URL, template);
    }

    @Test
    public void testRetrieveEvents() {
        final List<Event> events = ImmutableList.of(new Event());
        when(bunch.getEvents()).thenReturn(events);

        final EventsWithNextPage result = retriever.retrieveEvents();

        verify(template).exchange(eq(PRODUCER_EVENTS_URL), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(BunchOfEvents.class));
        assertNotNull(result);
        assertThat(result.getEvents(), is(events));
    }

    @Test
    public void testNextPageExisting() {

        // first page with next link
        // setup
        final String nextUrl = "nextUrl";
        when(bunch.getLinks()).thenReturn(makeNextLink(nextUrl));

        // do stuff
        final EventsWithNextPage firstResult = retriever.retrieveEvents();

        // second page with event
        // setup
        final List<Event> events = ImmutableList.of(new Event());
        when(bunch.getEvents()).thenReturn(events);

        // do stuff
        final Optional<EventsWithNextPage> secondResultOpt = firstResult.nextPage();

        // asserts
        assertTrue(secondResultOpt.isPresent());
        assertThat(secondResultOpt.get().getEvents(), is(events));
        verify(template).exchange(eq(nextUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(BunchOfEvents.class));
    }

    private BunchofEventsLinks makeNextLink(final String nextUrl) {
        final BunchofEventsLinks links = new BunchofEventsLinks();
        final BunchofEventsLinksNext next = new BunchofEventsLinksNext();
        next.setHref(nextUrl);
        links.setNext(next);
        return links;
    }

    @Test
    public void testNextPageNoLinks() {
        when(bunch.getLinks()).thenReturn(null);

        // do stuff
        final EventsWithNextPage firstResult = retriever.retrieveEvents();
        final Optional<EventsWithNextPage> secondResultOpt = firstResult.nextPage();

        assertFalse(secondResultOpt.isPresent());
        verify(template).exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(BunchOfEvents.class));
    }

    @Test
    public void testNextPageLinksNoNext() {
        when(bunch.getLinks()).thenReturn(new BunchofEventsLinks());

        // do stuff
        final EventsWithNextPage firstResult = retriever.retrieveEvents();
        final Optional<EventsWithNextPage> secondResultOpt = firstResult.nextPage();

        assertFalse(secondResultOpt.isPresent());
        verify(template).exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(BunchOfEvents.class));
    }

    @Test
    public void testNextPageLinksNextNoHref() {
        when(bunch.getLinks()).thenReturn(makeNextLink(null));

        // do stuff
        final EventsWithNextPage firstResult = retriever.retrieveEvents();
        final Optional<EventsWithNextPage> secondResultOpt = firstResult.nextPage();

        assertFalse(secondResultOpt.isPresent());
        verify(template).exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(BunchOfEvents.class));
    }

}
