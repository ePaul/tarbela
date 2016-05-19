package org.zalando.tarbela.nakadi;

import static org.springframework.http.HttpStatus.MULTI_STATUS;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.REDIRECTION;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import static org.zalando.riptide.Conditions.anyContentType;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.series;
import static org.zalando.riptide.Selectors.status;

import static org.zalando.tarbela.util.StringConstants.CONTENT_TYPE_PROBLEM_JSON;

import java.io.IOException;

import java.net.URI;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import org.springframework.web.client.RestTemplate;

import org.zalando.riptide.PassThroughResponseErrorHandler;
import org.zalando.riptide.Rest;
import org.zalando.riptide.Selectors;

import org.zalando.tarbela.nakadi.models.BatchItemResponse;
import org.zalando.tarbela.nakadi.models.Problem;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NakadiClientImpl implements NakadiClient {

    @SuppressWarnings("serial")
    private static final TypeToken<List<BatchItemResponse>> BATCH_RESULT_TYPE_TOKEN =
        new TypeToken<List<BatchItemResponse>>() { };

    private static final MediaType PROBLEM_MEDIA_TYPE = MediaType.parseMediaType(CONTENT_TYPE_PROBLEM_JSON);

    private final String submissionUriTemplate;
    private final Rest rest;

    public NakadiClientImpl(final String nakadiURI, final RestTemplate template) {
        this.submissionUriTemplate = nakadiURI;

        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.rest = Rest.create(template);
    }

    private static Problem unknownProblem(final ClientHttpResponse response) throws IOException {
        final Problem p = new Problem();
        p.setStatus(response.getRawStatusCode());
        p.setTitle(response.getStatusText());
        p.setDetail("Could not parse Problem from HTTP response");
        return p;
    }

    @Override
    public void submitEvents(final String eventType, final List<? extends Map<String, Object>> events,
            final NakadiResponseCallback callback) {

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(ImmutableList.of(PROBLEM_MEDIA_TYPE, APPLICATION_JSON, ALL));

        // TODO: make this a little bit more robust/performant
        final URI url = URI.create(submissionUriTemplate.replaceFirst("\\{.*\\}", eventType));

        submitToURI(events, callback, headers, url);
    }

    private void submitToURI(final List<? extends Map<String, Object>> events, final NakadiResponseCallback callback,
            final HttpHeaders headers, final URI url) {
        log.info("submitting {} events to {} ...", events.size(), url);
        rest.execute(HttpMethod.POST, url, headers, events) //
            .dispatch(series(),
                on(SUCCESSFUL).dispatch(status(),
                    on(MULTI_STATUS, BATCH_RESULT_TYPE_TOKEN).call(callback::partiallySubmitted),
                    on(OK).call(response -> callback.successfullyPublished()), //
                    anyStatus().call(response -> callback.otherSuccessStatus(response.getStatusCode()))),
                on(REDIRECTION).call(response -> {
                    final URI redirectUrl = response.getHeaders().getLocation();

                    // TODO: handle redirect loops? Or let the
                    // StackOverflowError handle this?
                    log.info("redirecting from {} to {}", url, redirectUrl);
                    submitToURI(events, callback, headers, redirectUrl);
                }),
                on(CLIENT_ERROR).dispatch(status(),
                    on(HttpStatus.UNPROCESSABLE_ENTITY, BATCH_RESULT_TYPE_TOKEN).call(callback::validationProblem),
                    anyStatus().dispatch(Selectors.contentType(),

                        // TODO: check if Nakadi actually sends application/problem+json back. Otherwise this won't work.
                        on(PROBLEM_MEDIA_TYPE, Problem.class).call(callback::clientError),
                        anyContentType().call(response -> callback.clientError(unknownProblem(response))))),
                on(Series.SERVER_ERROR).dispatch(Selectors.contentType(),
                    on(PROBLEM_MEDIA_TYPE, Problem.class).call(callback::serverError), //
                    anyContentType().call(response -> callback.serverError(unknownProblem(response)))));
    }
}
