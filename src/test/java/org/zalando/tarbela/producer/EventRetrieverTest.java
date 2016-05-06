package org.zalando.tarbela.producer;

import static org.junit.Assert.assertNotNull;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.doReturn;

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
        final EventsWithNextPage result = retriever.retrieveEvents();
        assertNotNull(result);
    }

}
