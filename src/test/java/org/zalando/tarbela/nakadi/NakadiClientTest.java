package org.zalando.tarbela.nakadi;

import static org.hamcrest.Matchers.is;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.test.web.client.MockRestServiceServer;

import org.springframework.web.client.RestTemplate;

import com.google.common.collect.ImmutableList;

public class NakadiClientTest {

    private static final String URI_TEMPLATE_STRING = "https://example.org/event-types/{type_id}/submit";
    private NakadiClient client;

    @Mock
    private NakadiResponseCallback callback;

    private MockRestServiceServer mockServer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final RestTemplate template = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(template);

        client = new NakadiClientImpl(URI_TEMPLATE_STRING, template);
    }

    @Test
    public void testSubmitEventsOnlyOneHappy() throws IOException {
        final String eventType = "exampleType";
        final String expectedUri = "https://example.org/event-types/exampleType/submit";
        final Map<String, Object> event = new HashMap<>();
        final List<? extends Map<String, Object>> events = ImmutableList.of(event);

        mockServer.expect(requestTo(expectedUri))                               //
                  .andExpect(method(HttpMethod.POST))                           //
                  .andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
                  .andExpect(jsonPath("$").value(is(events)))                   //
                  .andRespond(withSuccess());

        client.submitEvents(eventType, events, callback);

        mockServer.verify();

        verify(callback).successfullyPublished();
        verifyNoMoreInteractions(callback);
    }

}
