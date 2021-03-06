package org.zalando.tarbela.service;

import static java.util.stream.Collectors.joining;

import static org.hamcrest.Matchers.is;

import static org.springframework.http.HttpStatus.MULTI_STATUS;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.parseMediaType;

import static org.springframework.test.web.client.MockRestServiceServer.createServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import static org.zalando.tarbela.nakadi.models.BatchItemResponse.PublishingStatusEnum.aborted;
import static org.zalando.tarbela.nakadi.models.BatchItemResponse.PublishingStatusEnum.failed;
import static org.zalando.tarbela.nakadi.models.BatchItemResponse.PublishingStatusEnum.submitted;
import static org.zalando.tarbela.util.StringConstants.CONTENT_TYPE_BUNCH_OF_EVENTS;
import static org.zalando.tarbela.util.StringConstants.CONTENT_TYPE_BUNCH_OF_EVENT_UPDATES;

import java.net.URI;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Bean;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

import org.springframework.web.client.RestTemplate;

import org.zalando.tarbela.nakadi.NakadiClient;
import org.zalando.tarbela.nakadi.NakadiClientImpl;
import org.zalando.tarbela.nakadi.models.BatchItemResponse.PublishingStatusEnum;
import org.zalando.tarbela.producer.EventRetriever;
import org.zalando.tarbela.producer.EventRetrieverImpl;
import org.zalando.tarbela.producer.EventStatusUpdater;
import org.zalando.tarbela.producer.EventStatusUpdaterImpl;
import org.zalando.tarbela.producer.models.Event;
import org.zalando.tarbela.producer.models.EventChannel;
import org.zalando.tarbela.util.EventPayload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = EventServiceIT.Config.class)
public class EventServiceIT {

    private static final String NAKADI_URI_TEMPLATE = "https://nakadi.example.org/event-types/{type_id}/events";
    private static final URI PRODUCER_URI = URI.create("https://producer.example.org/events");
    private static final MediaType PRODUCER_EVENT_MEDIA_TYPE = parseMediaType(CONTENT_TYPE_BUNCH_OF_EVENTS);
    private static final MediaType PRODUCER_EVENT_UPDATE_MEDIA_TYPE = //
        parseMediaType(CONTENT_TYPE_BUNCH_OF_EVENT_UPDATES);

    @Autowired
    private RestTemplate producerGetRestTemplate;
    @Autowired
    private RestTemplate producerPatchRestTemplate;
    @Autowired
    private RestTemplate nakadiRestTemplate;

    // We can't have those as beans, because they need to be reinitialized for each expect/verify cycle.
    //
    // We are using three servers (with three templates), because with just one the server enforces the
    // same order of the requests as in the expects.  https://jira.spring.io/browse/SPR-11365 introduces
    // an .ignoreExpectOrder() which makes things more flexible, but this will only appear in version 4.3,
    // which is not yet released (as of 2016-0-19) – I don't want to use the release candidates.
    private MockRestServiceServer producerGetMock;
    private MockRestServiceServer producerPatchMock;
    private MockRestServiceServer nakadiMock;

    private ObjectMapper jsonMapper;

    @Autowired
    private EventService service;

    @Before
    public void setUp() throws Exception {
        producerGetMock = createServer(producerGetRestTemplate);
        producerPatchMock = createServer(producerPatchRestTemplate);
        nakadiMock = createServer(nakadiRestTemplate);
        jsonMapper = new ObjectMapper();
    }

    @Test
    public void doNothingAsThereAreNoEvents() {
        setupRetrieverMockWithPages();

        service.publishEvents();

        producerGetMock.verify();
        producerPatchMock.verify();
        nakadiMock.verify();
    }

    @Test
    public void happyCaseWithTwoPagesAndSeveralEventTypes() {
        setupRetrieverMockWithPages( //
            ImmutableList.of(makeEvent("A", "1"), makeEvent("A", "2"), makeEvent("B", "3")),
            ImmutableList.of(makeEvent("B", "4"), makeEvent("A", "5"), makeEvent("B", "6")));

        expectNakadiCall("A", event("A", "1"), event("A", "2")).andRespond(withSuccess());
        expectNakadiCall("B", event("B", "3")).andRespond(withSuccess());
        expectNakadiCall("A", event("A", "5")).andRespond(withSuccess());
        expectNakadiCall("B", event("B", "4"), event("B", "6")).andRespond(withSuccess());

        expectProducerUpdateCall(update("1", "SENT"), update("2", "SENT")).andRespond(withSuccess());
        expectProducerUpdateCall(update("3", "SENT")).andRespond(withSuccess());
        expectProducerUpdateCall(update("5", "SENT")).andRespond(withSuccess());
        expectProducerUpdateCall(update("4", "SENT"), update("6", "SENT")).andRespond(withSuccess());

        service.publishEvents();

        producerGetMock.verify();
        producerPatchMock.verify();
        nakadiMock.verify();
    }

    @Test
    public void partialFailures() {
        setupRetrieverMockWithPages( //
            ImmutableList.of(makeEvent("A", "1"), makeEvent("A", "2"), makeEvent("A", "3")),
            ImmutableList.of(makeEvent("A", "4"), makeEvent("A", "5"), makeEvent("A", "6")));

        expectNakadiCall("A", event("A", "1"), event("A", "2"), event("A", "3")) //
        .andRespond(withPartialSuccess(submitted, failed, aborted));
        expectNakadiCall("A", event("A", "4"), event("A", "5"), event("A", "6")) //
        .andRespond(withValidationFailure(aborted, failed, aborted));

        expectProducerUpdateCall(update("1", "SENT"), update("2", "ERROR")).andRespond(withSuccess());
        expectProducerUpdateCall(update("5", "ERROR")).andRespond(withSuccess());

        service.publishEvents();

        producerGetMock.verify();
        producerPatchMock.verify();
        nakadiMock.verify();
    }

    private static ResponseCreator withPartialSuccess(final PublishingStatusEnum... statuses) {
        return MockRestResponseCreators.withStatus(MULTI_STATUS).contentType(APPLICATION_JSON).body(
                formatAsBatchItemResponses(statuses));
    }

    private static ResponseCreator withValidationFailure(final PublishingStatusEnum... statuses) {
        return MockRestResponseCreators.withStatus(UNPROCESSABLE_ENTITY).contentType(APPLICATION_JSON).body(
                formatAsBatchItemResponses(statuses));
    }

    private static String formatAsBatchItemResponses(final PublishingStatusEnum[] statuses) {
        return Stream.of(statuses)                                                //
                     .map(status -> "{\"publishing_status\":\"" + status + "\"}") //
                     .collect(joining(",", "[", "]"));
    }

    //J- Jalopy removes the `final`, but the compiler needs it for `@SafeVarargs`.
    @SafeVarargs
    private final ResponseActions
    //J+
    expectNakadiCall(final String eventType, final Map<String, Object>... expectedEvents) {
        return nakadiMock.expect(requestTo(NAKADI_URI_TEMPLATE.replace("{type_id}", eventType)))
                         .andExpect(jsonPath("$").value(is(Arrays.asList(expectedEvents)))).andExpect(content()
                                 .contentType(MediaType.APPLICATION_JSON));
    }

    //J- Jalopy removes the `final`, but the compiler needs it for `@SafeVarargs`.
    @SafeVarargs
    private final ResponseActions
    //J+
    expectProducerUpdateCall(final Map<String, String>... expectedUpdates) {
        return producerPatchMock.expect(requestTo(PRODUCER_URI))
                                .andExpect(jsonPath("$").value(
                                        is(ImmutableMap.of("events", Arrays.asList(expectedUpdates))))).andExpect(
                                    content().contentType(PRODUCER_EVENT_UPDATE_MEDIA_TYPE));
    }

    private Map<String, String> update(final String eid, final String status) {
        return ImmutableMap.of("event_id", eid, "delivery_status", status);
    }

    private Map<String, Object> event(final String type, final String id) {
        return ImmutableMap.of("type", type, "id", id);
    }

    private Event makeEvent(final String eventType, final String eId) {
        final EventChannel channel = new EventChannel();
        channel.setTopicName(eventType);
        channel.setSinkIdentifier("zalando-nakadi");

        final EventPayload payload = new EventPayload();
        payload.put("type", eventType);
        payload.put("id", eId);

        final Event e = new Event();
        e.setChannel(channel);
        e.setEventId(eId);
        e.setEventPayload(payload);
        return e;
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
        URI currentUrl = PRODUCER_URI;

        int counter = 1;

        for (final List<Event> list : pages) {
            final URI nextUrl = URI.create(PRODUCER_URI + "?cursor=" + counter);
            final String body = formatEventsAsJson(list, nextUrl);
            producerGetMock.expect(MockRestRequestMatchers.requestTo(currentUrl))
                           .andExpect(MockRestRequestMatchers.method(HttpMethod.GET)).andRespond(
                               MockRestResponseCreators.withSuccess(body, PRODUCER_EVENT_MEDIA_TYPE));
            currentUrl = nextUrl;
            counter++;
        }

        producerGetMock.expect(MockRestRequestMatchers.requestTo(currentUrl)).andRespond(MockRestResponseCreators
                .withSuccess("{\"events\": [] }", PRODUCER_EVENT_MEDIA_TYPE));
    }

    private String formatEventsAsJson(final List<Event> events, final URI nextLink) {
        try {
            return jsonMapper.writeValueAsString(ImmutableMap.of("events", events, "_links",
                        ImmutableMap.of("next", ImmutableMap.of("href", nextLink.toString()))));
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    static class Config {

        @Bean
        ObjectMapper jackson() {
            return new ObjectMapper();
        }

        @Bean
        RestTemplate nakadiRestTemplate() {
            return new RestTemplate();
        }

        @Bean
        RestTemplate producerGetRestTemplate() {
            return new RestTemplate();
        }

        @Bean
        RestTemplate producerPatchRestTemplate() {
            return new RestTemplate();
        }

        @Bean
        NakadiClient nakadiClient(final RestTemplate nakadiRestTemplate) {
            return new NakadiClientImpl(NAKADI_URI_TEMPLATE, nakadiRestTemplate);
        }

        @Bean
        EventRetriever eventRetriever(final RestTemplate producerGetRestTemplate) {
            return new EventRetrieverImpl(PRODUCER_URI, producerGetRestTemplate);
        }

        @Bean
        EventStatusUpdater eventStatusUpdater(final RestTemplate producerPatchRestTemplate) {
            return new EventStatusUpdaterImpl(PRODUCER_URI, producerPatchRestTemplate);
        }

        @Bean
        EventService eventService(final EventRetriever eventRetriever, final EventStatusUpdater eventStatusUpdater,
                                  final NakadiClient nakadiClient) {
            return new EventService(eventRetriever, eventStatusUpdater, nakadiClient);
        }
    }

}
