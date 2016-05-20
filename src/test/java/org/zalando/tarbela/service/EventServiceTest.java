package org.zalando.tarbela.service;

import static org.junit.Assert.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.zalando.tarbela.nakadi.models.BatchItemResponse.PublishingStatusEnum.aborted;
import static org.zalando.tarbela.nakadi.models.BatchItemResponse.PublishingStatusEnum.failed;
import static org.zalando.tarbela.nakadi.models.BatchItemResponse.PublishingStatusEnum.submitted;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

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

import org.springframework.http.HttpStatus;

import org.zalando.tarbela.nakadi.NakadiClient;
import org.zalando.tarbela.nakadi.NakadiResponseCallback;
import org.zalando.tarbela.nakadi.models.BatchItemResponse;
import org.zalando.tarbela.nakadi.models.BatchItemResponse.PublishingStatusEnum;
import org.zalando.tarbela.nakadi.models.Problem;
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
    private static final ImmutableMap<String, Object> PAYLOAD_1 = ImmutableMap.of("hello", "World");
    private static final ImmutableMap<String, Object> PAYLOAD_2 = ImmutableMap.of("hello", "other world");

    @Mock
    private EventRetriever retriever;
    @Mock
    private NakadiClient nakadiClient;
    @Mock
    private EventStatusUpdater updater;

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
    }

    @Test
    public void testHappyCaseTwoEventsSameTypeAreSubmittedTogether() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_1, "2", PAYLOAD_2)));
        setupNakadiMock(callback -> { });

        // test
        service.publishEvents();

        // asserts
        verify(nakadiClient).submitEvents(eq(EVENT_TYPE_1), eq(ImmutableList.of(PAYLOAD_1, PAYLOAD_2)),
            any(NakadiResponseCallback.class));
    }

    @Test
    public void testHappyCaseTwoEventsSameTypeAreUpdatedTogether() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_1, "2", PAYLOAD_2)));
        setupNakadiMock(callback -> callback.successfullyPublished());
        setupUpdaterMock();

        // test
        service.publishEvents();

        // asserts
        assertThat(updatesCaptor.getValue(),
            containsInAnyOrder(updateWithIdAndStatus("1", "SENT"), updateWithIdAndStatus("2", "SENT")));
    }

    @Test
    public void testHappyCaseUnexpectedSuccessResult() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_1, "2", PAYLOAD_2)));
        setupNakadiMock(callback -> callback.otherSuccessStatus(HttpStatus.ACCEPTED));
        setupUpdaterMock();

        // test
        service.publishEvents();

        // asserts
        assertThat(updatesCaptor.getValue(),
            containsInAnyOrder(updateWithIdAndStatus("1", "SENT"), updateWithIdAndStatus("2", "SENT")));
    }

    @Test
    public void testHappyCaseTwoPagesAreSubmittedSeparately() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_1, "2", PAYLOAD_2)),
            ImmutableList.of(makeEvent(EVENT_TYPE_1, "3", PAYLOAD_1), makeEvent(EVENT_TYPE_1, "4", PAYLOAD_2)));
        setupNakadiMock(callback -> { });

        // test
        service.publishEvents();

        // asserts
        verify(nakadiClient, times(2)).submitEvents(eq(EVENT_TYPE_1), eq(ImmutableList.of(PAYLOAD_1, PAYLOAD_2)),
            any(NakadiResponseCallback.class));
    }

    @Test
    public void testHappyCaseTwoPagesAreUpdatedSeparately() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_1, "2", PAYLOAD_2)),
            ImmutableList.of(makeEvent(EVENT_TYPE_1, "3", PAYLOAD_1), makeEvent(EVENT_TYPE_1, "4", PAYLOAD_2)));
        setupNakadiMock(callback -> callback.successfullyPublished());
        setupUpdaterMock();

        // test
        service.publishEvents();

        // asserts
        verify(updater, times(2)).updateStatuses(anyListOf(EventUpdate.class));
        assertThat(updatesCaptor.getAllValues(),
            containsInAnyOrder(
                containsInAnyOrder(updateWithIdAndStatus("1", "SENT"), updateWithIdAndStatus("2", "SENT")),
                containsInAnyOrder(updateWithIdAndStatus("3", "SENT"), updateWithIdAndStatus("4", "SENT"))));
    }

    @Test
    public void testHappyCaseTwoEventsDifferentTypeAreSubmittedSeparately() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_2, "2", PAYLOAD_2)));
        setupNakadiMock(callback -> { });

        // test
        service.publishEvents();

        // asserts
        verify(nakadiClient).submitEvents(eq(EVENT_TYPE_1), eq(ImmutableList.of(PAYLOAD_1)),
            any(NakadiResponseCallback.class));
        verify(nakadiClient).submitEvents(eq(EVENT_TYPE_2), eq(ImmutableList.of(PAYLOAD_2)),
            any(NakadiResponseCallback.class));
    }

    @Test
    public void testHappyCaseTwoEventsDifferentTypeAreUpdatedSeparately() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_2, "2", PAYLOAD_2)));
        setupNakadiMock(callback -> callback.successfullyPublished());
        setupUpdaterMock();

        // test
        service.publishEvents();

        // asserts
        verify(updater, times(2)).updateStatuses(anyListOf(EventUpdate.class));
        assertThat(updatesCaptor.getAllValues(),
            containsInAnyOrder(contains(updateWithIdAndStatus("1", "SENT")),
                contains(updateWithIdAndStatus("2", "SENT"))));
    }

    @Test
    public void testValidationProblem() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_1, "2", PAYLOAD_2)));

        final List<BatchItemResponse> responses = ImmutableList.of(makeResponse(aborted),
                makeResponse(failed, "spaces are not allowed!"));
        setupNakadiMock(callback -> callback.validationProblem(responses));
        setupUpdaterMock();

        // test
        service.publishEvents();

        // assert
        assertThat(updatesCaptor.getAllValues(), contains(contains(updateWithIdAndStatus("2", "ERROR"))));
    }

    @Test
    public void testValidationProblemAllAborted() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_1, "2", PAYLOAD_2)));

        final List<BatchItemResponse> responses = ImmutableList.of(makeResponse(aborted), makeResponse(aborted));
        setupNakadiMock(callback -> callback.validationProblem(responses));
        setupUpdaterMock();

        // test
        service.publishEvents();

        // assert
        verify(updater, never()).updateStatuses(anyListOf(EventUpdate.class));
    }

    @Test
    public void testPartiallySubmitted() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_1, "2", PAYLOAD_2)));

        final List<BatchItemResponse> responses = ImmutableList.of(makeResponse(submitted),
                makeResponse(failed, "spaces are not allowed!"));
        setupNakadiMock(callback -> callback.partiallySubmitted(responses));
        setupUpdaterMock();

        // test
        service.publishEvents();

        // assert
        assertThat(updatesCaptor.getAllValues(),
            contains(containsInAnyOrder(updateWithIdAndStatus("1", "SENT"), updateWithIdAndStatus("2", "ERROR"))));
    }

    @Test
    public void testServerError() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_1, "2", PAYLOAD_2)));

        final Problem problem = makeProblem();
        setupNakadiMock(callback -> callback.serverError(problem));
        setupUpdaterMock();

        // test
        service.publishEvents();

        // assert
        verify(updater, never()).updateStatuses(anyListOf(EventUpdate.class));
    }

    @Test
    public void testClientError() {

        // setup
        setupRetrieverMockWithPages(ImmutableList.of(makeEvent(EVENT_TYPE_1, "1", PAYLOAD_1),
                makeEvent(EVENT_TYPE_1, "2", PAYLOAD_2)));

        final Problem problem = makeProblem();
        setupNakadiMock(callback -> callback.clientError(problem));
        setupUpdaterMock();

        // test
        service.publishEvents();

        // assert
        verify(updater, never()).updateStatuses(anyListOf(EventUpdate.class));
    }

    // ---------- Mock setup --------

    private Problem makeProblem() {
        final Problem result = new Problem();

        // TODO: fill relevant fields?
        return result;
    }

    /**
     * Sets up the mock for the updater to accept anything and capture it in the updatesCaptor.
     */
    private void setupUpdaterMock() {
        doNothing().when(updater).updateStatuses(updatesCaptor.capture());
    }

    /**
     * Sets up the retriever mock to return a page with the first argument's list of events, with possibly the following
     * arguments as the next pages.
     */
    //J- Jalopy insists on removing the final, while the compiler insists of it being here.
    @SafeVarargs
    private final void
    //J+
    setupRetrieverMockWithPages(final List<Event>... pages) {
        EventsWithNextPage currentPage = mock(EventsWithNextPage.class);
        when(retriever.retrieveEvents()).thenReturn(currentPage);

        EventsWithNextPage previousPage = null;
        for (final List<Event> list : pages) {
            when(currentPage.getEvents()).thenReturn(list);

            final EventsWithNextPage next = mock(EventsWithNextPage.class);
            when(currentPage.nextPage()).thenReturn(Optional.of(next));
            previousPage = currentPage;
            currentPage = next;
        }

        when(previousPage.nextPage()).thenReturn(Optional.empty());
    }

    /**
     * Sets up the Nakadi client mock to accept any submit calls, capturing the events and passing the callback to the
     * given callback consumer (which is expected to call one of the callback's methods).
     *
     * @param  callbackConsumer
     */
    private void setupNakadiMock(final Consumer<NakadiResponseCallback> callbackConsumer) {
        doAnswer(invocation -> {
                                    callbackConsumer.accept(callbackCaptor.getValue());
                                    return null;
                                }).when(nakadiClient).submitEvents(anyString(), nakadiEventsCaptor.capture(),
                                    callbackCaptor.capture());
    }

    // ------------ test data ---------

    private BatchItemResponse makeResponse(final PublishingStatusEnum status, final String detail) {
        final BatchItemResponse result = new BatchItemResponse();
        result.setPublishingStatus(status);
        result.setDetail(detail);
        return result;
    }

    private BatchItemResponse makeResponse(final PublishingStatusEnum status) {
        return makeResponse(status, null);
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

    // -------- matchers ---------

    @SafeVarargs
    private static <X> Matcher<Iterable<? extends X>> containsInAnyOrder(final Matcher<X>... matchers) {
        return Matchers.containsInAnyOrder(matchers);
    }

    @SafeVarargs
    private static <X> Matcher<Iterable<? extends X>> contains(final Matcher<X>... matchers) {
        return Matchers.contains(matchers);
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
