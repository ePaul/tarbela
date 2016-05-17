package org.zalando.tarbela.service;

import static org.hamcrest.Matchers.contains;

import static org.junit.Assert.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.zalando.tarbela.nakadi.NakadiClient;
import org.zalando.tarbela.nakadi.NakadiResponseCallback;
import org.zalando.tarbela.producer.EventRetriever;
import org.zalando.tarbela.producer.EventStatusUpdater;
import org.zalando.tarbela.producer.EventsWithNextPage;
import org.zalando.tarbela.producer.models.Event;
import org.zalando.tarbela.producer.models.EventChannel;
import org.zalando.tarbela.producer.models.EventUpdate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class EventServiceTest {

    private static final String EVENT_TYPE_1 = "exampleHappened";
    private static final String EVENT_TYPE_2 = "DataChanged";

    @Mock
    private EventRetriever retriever;
    @Mock
    private NakadiClient nakadiClient;
    @Mock
    private EventStatusUpdater updater;
    @Mock
    private EventsWithNextPage firstPage;
    @Mock
    private EventsWithNextPage secondPage;

    @Captor
    private ArgumentCaptor<NakadiResponseCallback> callbackCaptor;
    @Captor
    private ArgumentCaptor<List<Map<String, Object>>> nakadiEventsCaptor;
    @Captor
    private ArgumentCaptor<List<EventUpdate>> updatesCaptor;

    @InjectMocks
    private EventService service = new EventServiceImpl();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(retriever.retrieveEvents()).thenReturn(firstPage);

    }

    @Test
    public void testHappyCaseTwoEventsSameType() {

        // setup
        final ImmutableMap<String, Object> payLoad1 = ImmutableMap.of("hello", "World");
        final ImmutableMap<String, Object> payLoad2 = ImmutableMap.of("hello", "other world");

        when(firstPage.getEvents()).thenReturn(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", payLoad1),
                makeEvent(EVENT_TYPE_1, "2", payLoad2)));
        when(firstPage.nextPage()).thenReturn(Optional.empty());
        doAnswer(invocation -> {
                                    callbackCaptor.getValue().successfullyPublished();
                                    return null;
                                }).when(nakadiClient).submitEvents(anyString(), nakadiEventsCaptor.capture(),
                                    callbackCaptor.capture());
        doNothing().when(updater).updateStatuses(updatesCaptor.capture());

        // test
        service.publishEvents();

        // asserts
        verify(nakadiClient).submitEvents(eq(EVENT_TYPE_1), eq(ImmutableList.of(payLoad1, payLoad2)),
            any(NakadiResponseCallback.class));
        assertThat(updatesCaptor.getValue(),
            containsInAnyOrder(updateWithIdAndStatus("1", "SENT"), updateWithIdAndStatus("2", "SENT")));
    }

    @Test
    public void testHappyCaseTwoEventsDifferentType() {

        // setup
        final ImmutableMap<String, Object> payLoad1 = ImmutableMap.of("hello", "World");
        final ImmutableMap<String, Object> payLoad2 = ImmutableMap.of("hello", "other world");

        when(firstPage.getEvents()).thenReturn(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", payLoad1),
                makeEvent(EVENT_TYPE_2, "2", payLoad2)));
        when(firstPage.nextPage()).thenReturn(Optional.empty());
        doAnswer(invocation -> {
                                    callbackCaptor.getValue().successfullyPublished();
                                    return null;
                                }).when(nakadiClient).submitEvents(anyString(), nakadiEventsCaptor.capture(),
                                    callbackCaptor.capture());
        doNothing().when(updater).updateStatuses(updatesCaptor.capture());

        // test
        service.publishEvents();

        // asserts
        verify(nakadiClient).submitEvents(eq(EVENT_TYPE_1), eq(ImmutableList.of(payLoad1)),
            any(NakadiResponseCallback.class));
        verify(nakadiClient).submitEvents(eq(EVENT_TYPE_2), eq(ImmutableList.of(payLoad2)),
            any(NakadiResponseCallback.class));
        verify(updater, times(2)).updateStatuses(anyListOf(EventUpdate.class));
        assertThat(updatesCaptor.getAllValues(),
            containsInAnyOrder(contains(updateWithIdAndStatus("1", "SENT")),
                contains(updateWithIdAndStatus("2", "SENT"))));
    }

    private Event makeEvent(final String eventType, final String eId, final Map<String, Object> payload) {
        final EventChannel channel = new EventChannel();
        channel.setTopicName(eventType);
        channel.setSinkIdentifier("zalando-nakadi");

        final Event e = new Event();
        e.setChannel(channel);
        e.setEventId(eId);
        e.setEventPayload(new HashMap<>(payload));
        return e;
    }

    @SafeVarargs
    private static <X> Matcher<Iterable<? extends X>> containsInAnyOrder(final Matcher<X>... matchers) {
        return Matchers.containsInAnyOrder(matchers);
    }

    private Matcher<EventUpdate> updateWithIdAndStatus(final String id, final String status) {
        return new TypeSafeMatcher<EventUpdate>() {

            @Override
            public void describeTo(final Description description) {
                description.appendText("EventUpdate( eid=" + id + ", status=" + status + ")");
            }

            @Override
            protected boolean matchesSafely(final EventUpdate item) {
                return item.getEventId().equals(id) && item.getDeliveryStatus().equals(status);
            }
        };
    }

}
