package org.zalando.tarbela.service;

import java.net.URI;

import java.util.List;

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
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

import org.springframework.web.client.RestTemplate;

import org.zalando.tarbela.nakadi.NakadiClient;
import org.zalando.tarbela.nakadi.NakadiClientImpl;
import org.zalando.tarbela.producer.EventRetriever;
import org.zalando.tarbela.producer.EventRetrieverImpl;
import org.zalando.tarbela.producer.EventStatusUpdater;
import org.zalando.tarbela.producer.EventStatusUpdaterImpl;
import org.zalando.tarbela.producer.models.Event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.collect.ImmutableMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = EventServiceIT.Config.class)
public class EventServiceIT {

    private static final String NAKADI_URI_TEMPLATE = "https://nakadi.example.org/event-types/{type_id}/events";
    private static final URI PRODUCER_URI = URI.create("https://producer.example.org/events");
    private static final MediaType PRODUCER_EVENT_MEDIA_TYPE = MediaType.parseMediaType(
            "application/x.tarbela.event-list+json");
    private static final MediaType PRODUCER_EVENT_UPDATE_MEDIA_TYPE = MediaType.parseMediaType(
            "application/x.tarbela.event-list-update+json");

    @Autowired
    MockRestServiceServer producerGetMock;
    @Autowired
    MockRestServiceServer producerPatchMock;
    @Autowired
    MockRestServiceServer nakadiMock;

    ObjectMapper jsonMapper;

    @Autowired
    EventService service;

    @Before
    public void setUp() throws Exception {
        // TODO: setup mocks/captures
    }

    @Test
    public void testDoNothingAsThereAreNoEvents() {
        setupRetrieverMockWithPages();

        service.publishEvents();

        producerGetMock.verify();
        producerPatchMock.verify();
        nakadiMock.verify();
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
            final URI nextUrl = PRODUCER_URI.resolve("?cursor=" + counter);
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
            return jsonMapper.writeValueAsString(ImmutableMap.of("events", events, "links",
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
        MockRestServiceServer producerGetMock(final RestTemplate producerGetRestTemplate) {
            return MockRestServiceServer.createServer(producerGetRestTemplate);
        }

        @Bean
        MockRestServiceServer producerPatchMock(final RestTemplate producerPatchRestTemplate) {
            return MockRestServiceServer.createServer(producerPatchRestTemplate);
        }

        @Bean
        MockRestServiceServer nakadiMock(final RestTemplate nakadiRestTemplate) {
            return MockRestServiceServer.createServer(nakadiRestTemplate);
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
        EventService eventService() {
            return new EventServiceImpl();
        }
    }

}
