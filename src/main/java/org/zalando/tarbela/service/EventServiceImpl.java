package org.zalando.tarbela.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;

import org.zalando.tarbela.nakadi.NakadiClient;
import org.zalando.tarbela.nakadi.NakadiResponseCallback;
import org.zalando.tarbela.nakadi.models.BatchItemResponse;
import org.zalando.tarbela.nakadi.models.Problem;
import org.zalando.tarbela.producer.EventRetriever;
import org.zalando.tarbela.producer.EventsWithNextPage;
import org.zalando.tarbela.producer.models.Event;
import org.zalando.tarbela.producer.models.EventChannel;

public class EventServiceImpl implements EventService {

    // TODO: have multiple retrievers later
    @Autowired
    private EventRetriever retriever;

    // TODO: have multiple Nakadis (or other sinks) later.
    @Autowired
    private NakadiClient nakadiClient;

    @Override
    public void publishEvents() {
        Optional<EventsWithNextPage> events = Optional.of(retriever.retrieveEvents());
        while (events.isPresent()) {
            final EventsWithNextPage page = events.get();
            publishPage(page.getEvents());
            events = page.nextPage();
        }
    }

    private void publishPage(final List<Event> events) {

        // TODO: for this to actually work (i.e. to create not just groups of
        // size one), EventChannel needs equals/hashCode implementations.
        final Map<EventChannel, List<Event>> grouped = events.stream().collect(groupingBy(Event::getChannel));
        for (final Entry<EventChannel, List<Event>> group : grouped.entrySet()) {

            final List<Event> eventList = group.getValue();
            final String topicName = group.getKey().getTopicName();
            submitGroupAndHandleResponse(eventList, topicName);
        }
    }

    private void submitGroupAndHandleResponse(final List<Event> eventList, final String topicName) {
        final List<HashMap<String, Object>> payloads = eventList.stream().map(Event::getEventPayload).collect(toList());
        nakadiClient.submitEvents(topicName, payloads, new NakadiResponseCallback() {

                @Override
                public void validationProblem(final List<BatchItemResponse> responseList) {
                    // TODO write failures back to producer?

                }

                @Override
                public void successfullyPublished() {
                    // TODO write success back to producer

                }

                @Override
                public void serverError(final Problem problem) {
                    // TODO wait a bit, try again?

                }

                @Override
                public void partiallySubmitted(final List<BatchItemResponse> responseList) {
                    // TODO write both successes and failures back to producer

                }

                @Override
                public void otherSuccessStatus(final HttpStatus status) {
                    // TODO log warning (or error so it gets seen?), but write
                    // success back to producer.

                }

                @Override
                public void clientError(final Problem problem) {
                    // TODO analyze problem, react accordingly.
                }
            });
    }

}
