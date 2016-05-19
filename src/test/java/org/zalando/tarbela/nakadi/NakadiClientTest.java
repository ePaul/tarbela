package org.zalando.tarbela.nakadi;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.MULTI_STATUS;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TEMPORARY_REDIRECT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.http.MediaType.parseMediaType;

import static org.springframework.test.web.client.MockRestServiceServer.createServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import static org.zalando.tarbela.nakadi.models.BatchItemResponse.PublishingStatusEnum.ABORTED;
import static org.zalando.tarbela.nakadi.models.BatchItemResponse.PublishingStatusEnum.FAILED;
import static org.zalando.tarbela.nakadi.models.BatchItemResponse.PublishingStatusEnum.SUBMITTED;
import static org.zalando.tarbela.util.StringConstants.CONTENT_TYPE_PROBLEM_JSON;

import java.net.URI;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.test.web.client.MockRestServiceServer;

import org.springframework.web.client.RestTemplate;

import org.zalando.tarbela.nakadi.models.BatchItemResponse;
import org.zalando.tarbela.nakadi.models.BatchItemResponse.StepEnum;
import org.zalando.tarbela.nakadi.models.Problem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class NakadiClientTest {

    private static final String EXAMPLE_EVENT_TYPE = "exampleType";
    private static final String URI_TEMPLATE_STRING = "https://example.org/event-types/{type_id}/events";
    private static final MediaType APPLICATION_PROBLEM_JSON = parseMediaType(CONTENT_TYPE_PROBLEM_JSON);

    private NakadiClient client;
    private MockRestServiceServer mockServer;

    @Mock
    private NakadiResponseCallback callback;

    @Captor
    ArgumentCaptor<List<BatchItemResponse>> batchItemCaptor;
    @Captor
    ArgumentCaptor<Problem> problemCaptor;
    @Captor
    ArgumentCaptor<HttpStatus> statusCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final RestTemplate template = new RestTemplate();
        mockServer = createServer(template);

        client = new NakadiClientImpl(URI_TEMPLATE_STRING, template);
    }

    @Test
    public void testSubmitEventsCheckRequest() {
        final URI expectedUri = URI.create("https://example.org/event-types/exampleType/events");
        final List<? extends Map<String, Object>> events = someEvents();

        mockServer.expect(requestTo(expectedUri))                     //
                  .andExpect(method(POST))                            //
                  .andExpect(content().contentType(APPLICATION_JSON)) //
                  .andExpect(jsonPath("$").value(is(events)))         //
                  .andRespond(withSuccess());

        client.submitEvents(EXAMPLE_EVENT_TYPE, events, callback);

        mockServer.verify();
    }

    @Test
    public void testSubmitEventsSuccessfullySubmitted() {
        mockServer.expect(anything()) //
                  .andRespond(withSuccess());

        client.submitEvents(EXAMPLE_EVENT_TYPE, someEvents(), callback);

        verify(callback).successfullyPublished();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testSubmitEventsMultiStatus() {
        final String batchResponses =
            ("[{`publishing_status`:`SUBMITTED`}, "
                    + "{`publishing_status`:`FAILED`, `step`:`PUBLISHING`,`detail`:`database unavailable`}]") //
            .replace('`', '"');

        mockServer.expect(anything()) //
                  .andRespond(withStatus(MULTI_STATUS).body(batchResponses).contentType(APPLICATION_JSON));

        client.submitEvents(EXAMPLE_EVENT_TYPE, someEvents(), callback);

        verify(callback).partiallySubmitted(batchItemCaptor.capture());
        verifyNoMoreInteractions(callback);

        final List<BatchItemResponse> result = batchItemCaptor.getValue();
        assertThat(result, hasSize(2));
        assertThat(result.get(0).getPublishingStatus(), is(SUBMITTED));
        assertThat(result.get(1).getPublishingStatus(), is(FAILED));
        assertThat(result.get(1).getStep(), is(StepEnum.PUBLISHING));
        assertThat(result.get(1).getDetail(), is("database unavailable"));
    }

    @Test
    public void testSubmitEventsValidationProblem() {
        final String batchResponses =
            ("[{`publishing_status`:`ABORTED`}, "
                    + "{`publishing_status`:`FAILED`, `step`:`VALIDATING`,`detail`:`not a data change event`}]") //
            .replace('`', '"');

        mockServer.expect(anything()) //
                  .andRespond(withStatus(UNPROCESSABLE_ENTITY).body(batchResponses).contentType(APPLICATION_JSON));

        client.submitEvents(EXAMPLE_EVENT_TYPE, someEvents(), callback);

        verify(callback).validationProblem(batchItemCaptor.capture());
        verifyNoMoreInteractions(callback);

        final List<BatchItemResponse> result = batchItemCaptor.getValue();
        assertThat(result, hasSize(2));
        assertThat(result.get(0).getPublishingStatus(), is(ABORTED));
        assertThat(result.get(1).getPublishingStatus(), is(FAILED));
        assertThat(result.get(1).getStep(), is(StepEnum.VALIDATING));
        assertThat(result.get(1).getDetail(), is("not a data change event"));
    }

    @Test
    public void testSubmitEventsServerError() {
        final String problemResponse =
            ("{        `type`:`http://httpstatus.es/503`, "          //
                    + "`title`:`Service Unavailable`, "              //
                    + "`status`:503, "                               //
                    + "`detail`:`Connection to DB timed out.`,"      //
                    + "`instance`:`flowid:R2hWuyMWTpeCu7WTgU9yHA`}") //
            .replace('`', '"');

        mockServer.expect(anything()) //
                  .andRespond(withStatus(SERVICE_UNAVAILABLE).body(problemResponse).contentType(
                          APPLICATION_PROBLEM_JSON));

        client.submitEvents(EXAMPLE_EVENT_TYPE, someEvents(), callback);

        verify(callback).serverError(problemCaptor.capture());
        verifyNoMoreInteractions(callback);

        final Problem problem = problemCaptor.getValue();
        assertThat(problem.getType(), is("http://httpstatus.es/503"));
        assertThat(problem.getInstance(), is("flowid:R2hWuyMWTpeCu7WTgU9yHA"));
    }

    @Test
    public void testSubmitEventsClientError() {
        final String problemResponse =
            ("{        `type`:`http://httpstatus.es/404`, "          //
                    + "`title`:`Not Found`, "                        //
                    + "`status`:403, "                               //
                    + "`detail`:`No such event type.`,"              //
                    + "`instance`:`flowid:R2hWuyMWTpeCu7WTgU9yHA`}") //
            .replace('`', '"');

        mockServer.expect(anything()) //
                  .andRespond(withStatus(HttpStatus.NOT_FOUND).body(problemResponse).contentType(
                          APPLICATION_PROBLEM_JSON));

        client.submitEvents(EXAMPLE_EVENT_TYPE, someEvents(), callback);

        verify(callback).clientError(problemCaptor.capture());
        verifyNoMoreInteractions(callback);

        final Problem problem = problemCaptor.getValue();
        assertThat(problem.getType(), is("http://httpstatus.es/404"));
        assertThat(problem.getInstance(), is("flowid:R2hWuyMWTpeCu7WTgU9yHA"));
    }

    @Test
    public void testSubmitEventsRedirection() {
        final URI expectedUri = URI.create("https://example.org/event-types/exampleType/events");
        final URI redirectionUri = URI.create("https://example.com/event-types/exampleType/events");

        mockServer.expect(requestTo(expectedUri)) //
                  .andExpect(method(POST))        //
                  .andRespond(withStatus(TEMPORARY_REDIRECT).location(redirectionUri));

        mockServer.expect(requestTo(redirectionUri)) //
                  .andExpect(method(POST))           //
                  .andRespond(withStatus(HttpStatus.OK));

        client.submitEvents(EXAMPLE_EVENT_TYPE, someEvents(), callback);

        mockServer.verify();

        verify(callback).successfullyPublished();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testSubmitEventsUnknownServerError() {
        final String content = "<h1>Service Unavailable</h1><p>Database can't be reached!</p>";
        mockServer.expect(anything()) //
                  .andRespond(withStatus(SERVICE_UNAVAILABLE).contentType(TEXT_HTML).body(content));

        client.submitEvents(EXAMPLE_EVENT_TYPE, someEvents(), callback);

        verify(callback).serverError(problemCaptor.capture());
        verifyNoMoreInteractions(callback);

        final Problem problem = problemCaptor.getValue();
        assertThat(problem.getStatus(), is(503));
    }

    @Test
    public void testSubmitEventsUnknownClientError() {
        final String content =
            "<h1>Unauthorized</h1><p>Please authenticate yourself before trying to submit anything!</p>";
        mockServer.expect(anything()) //
                  .andRespond(withStatus(UNAUTHORIZED).contentType(TEXT_HTML).body(content));

        client.submitEvents(EXAMPLE_EVENT_TYPE, someEvents(), callback);

        verify(callback).clientError(problemCaptor.capture());
        verifyNoMoreInteractions(callback);

        final Problem problem = problemCaptor.getValue();
        assertThat(problem.getStatus(), is(401));
    }

    @Test
    public void testSubmitEventsUnknownSuccessStatus() {
        final String content = "<h1>Accepted</h1><p>The request was accepted and will be processed later.</p>";
        mockServer.expect(anything()) //
                  .andRespond(withStatus(ACCEPTED).contentType(TEXT_HTML).body(content));

        client.submitEvents(EXAMPLE_EVENT_TYPE, someEvents(), callback);

        verify(callback).otherSuccessStatus(statusCaptor.capture());
        verifyNoMoreInteractions(callback);

        assertThat(statusCaptor.getValue(), is(ACCEPTED));
    }

    private ImmutableList<ImmutableMap<String, Object>> someEvents() {
        return ImmutableList.of(warehouseCreateEvent(), businessEvent());
    }

    private ImmutableMap<String, Object> businessEvent() {
        //J-
        return ImmutableMap.of("order_number", "17",
                               "metadata", ImmutableMap.of("eid", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                                           "occurred_at", "2016-03-15T23:47:15+01:00"));
        //J+
    }

    private ImmutableMap<String, Object> warehouseCreateEvent() {
        //J-
        return ImmutableMap.of("data", ImmutableMap.of("name", "Example warehouse",
                                                       "address", ImmutableMap.of("street", "Example street",
                                                                                  "zip", 12345)),
                               "data_op", "C",
                               "metadata", ImmutableMap.of("eid", "6d78796c-9fe2-42e0-96f9-72ef406a824b",
                                                           "occured_at", "2016-05-12T13:27:05"));
        //J+
    }

}
