package org.zalando.tarbela.producer;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.MediaType.parseMediaType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import static org.zalando.tarbela.util.StringConstants.CONTENT_TYPE_BUNCH_OF_EVENT_UPDATES;

import java.net.URI;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;

import org.springframework.test.web.client.MockRestServiceServer;

import org.springframework.web.client.RestTemplate;

import org.zalando.tarbela.producer.models.EventUpdate;
import org.zalando.tarbela.test_util.CapturingMatcher;

import com.google.common.collect.ImmutableList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventStatusUpdaterTest {

    private static final URI PRODUCER_EVENTS_URI = URI.create("https://example.org/events");
    private static final MediaType UPDATE_MEDIA_TYPE = parseMediaType(CONTENT_TYPE_BUNCH_OF_EVENT_UPDATES);

    private EventStatusUpdater updater;

    private MockRestServiceServer mockServer;
    private CapturingMatcher<String> contentCapturer = new CapturingMatcher<String>() { };

    @Before
    public void setUp() throws Exception {
        final RestTemplate template = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(template);

        updater = new EventStatusUpdaterImpl(PRODUCER_EVENTS_URI, template);
    }

    @Test
    public void testUpdateStatuses() {
        final String id = "id1";
        final String status = "status1";
        final List<EventUpdate> updates = ImmutableList.of(update(id, status));

        mockServer.expect(requestTo(PRODUCER_EVENTS_URI))                           //
                  .andExpect(method(PATCH))                                         //
                  .andExpect(content().contentType(UPDATE_MEDIA_TYPE))              //
                  .andExpect(content().string(contentCapturer))                     //
                  .andExpect(jsonPath("$.events").isArray())                        //
                  .andExpect(jsonPath("$.events[0].event_id").value(id))            //
                  .andExpect(jsonPath("$.events[0].delivery_status").value(status)) //
                  .andRespond(withSuccess());

        updater.updateStatuses(updates);

        mockServer.verify();
        log.info("sent body: »{}«", contentCapturer.getValue());
    }

    private EventUpdate update(final String id, final String status) {
        final EventUpdate update = new EventUpdate();
        update.setEventId(id);
        update.setDeliveryStatus(status);
        return update;
    }
}
