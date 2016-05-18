package org.zalando.tarbela.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;

import org.zalando.tarbela.nakadi.NakadiClient;
import org.zalando.tarbela.nakadi.NakadiResponseCallback;
import org.zalando.tarbela.nakadi.models.BatchItemResponse;
import org.zalando.tarbela.nakadi.models.Problem;
import org.zalando.tarbela.producer.EventRetriever;
import org.zalando.tarbela.producer.EventStatusUpdater;
import org.zalando.tarbela.producer.EventsWithNextPage;
import org.zalando.tarbela.producer.models.Event;
import org.zalando.tarbela.producer.models.EventUpdate;
import org.zalando.tarbela.util.ZipUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventServiceImpl implements EventService {

    private static final String DELIVERY_STATUS_SENT = "SENT";
    private static final String DELIVERY_STATUS_ERROR = "ERROR";

    // TODO: have multiple retrievers later
    @Autowired
    private EventRetriever retriever;

    // TODO: have multiple Nakadis (or other sinks) later.
    @Autowired
    private NakadiClient nakadiClient;

    @Autowired
    private EventStatusUpdater updater;

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

        // instead of grouping by channel (which produces groups of size one, because EventChannel doesn't implement
        // equals/hashCode), we group by channel's toString (which actually works for the generated classes).
        final Iterable<List<Event>> grouped = events.stream().collect(groupingBy(event ->
                        event.getChannel().toString())).values();
        for (final List<Event> group : grouped) {
            final String topicName = group.get(0).getChannel().getTopicName();
            submitGroupAndHandleResponse(group, topicName);
        }
    }

    private void submitGroupAndHandleResponse(final List<Event> eventList, final String topicName) {
        final List<HashMap<String, Object>> payloads = eventList.stream().map(Event::getEventPayload).collect(toList());
        nakadiClient.submitEvents(topicName, payloads, new NakadiResponseCallback() {

                @Override
                public void validationProblem(final List<BatchItemResponse> responseList) {
                    log.info("no events were submitted, due to validation problems");

                    // write failures back to producer
                    createAndSendUpdatesFromBatchItemResponses(eventList, topicName, responseList);
                }

                @Override
                public void successfullyPublished() {
                    log.info("{} events were successfully published to {}", eventList.size(), topicName);

                    final List<EventUpdate> updates = eventList.stream().map(event -> {
                                                                   final EventUpdate update = new EventUpdate();
                                                                   update.setEventId(event.getEventId());
                                                                   update.setDeliveryStatus(DELIVERY_STATUS_SENT);
                                                                   return update;
                                                               }).collect(toList());

                    // write success back to producer
                    updater.updateStatuses(updates);
                }

                @Override
                public void serverError(final Problem problem) {
                    log.error("Some server error occurred: {}", problem);
                    // TODO wait a bit, try again?
                }

                @Override
                public void partiallySubmitted(final List<BatchItemResponse> responseList) {
                    log.info("some events were submitted, but not all of them.");
                    createAndSendUpdatesFromBatchItemResponses(eventList, topicName, responseList);
                }

                private void createAndSendUpdatesFromBatchItemResponses(final List<Event> eventList,
                        final String topicName, final List<BatchItemResponse> responseList) {
                    final List<? extends EventUpdate> updates = ZipUtils.mapPairs(eventList, responseList,
                            (event, response) -> {
                                final EventUpdate update = new EventUpdate();
                                update.setEventId(event.getEventId());
                                switch (response.getPublishingStatus()) {

                                    case ABORTED :

                                        // don't change the status.
                                        break;

                                    case FAILED :
                                        update.setDeliveryStatus(DELIVERY_STATUS_ERROR);
                                        log.error("event {} (with payload {}) could not be published to {}: {}",
                                            event.getEventId(), event.getEventPayload(), topicName, response);
                                        break;

                                    case SUBMITTED :
                                        update.setDeliveryStatus(DELIVERY_STATUS_SENT);
                                        break;

                                    default :
                                        throw new IllegalArgumentException(
                                            "unexpected publishing status: " + response.getPublishingStatus());
                                }

                                return update;
                            });

                    // this removes the updates relating to publishing status ABORTED.
                    updates.removeIf(update -> update.getDeliveryStatus() == null);

                    if (!updates.isEmpty()) {
                        updater.updateStatuses(updates);
                    }
                }

                @Override
                public void otherSuccessStatus(final HttpStatus status) {

                    // log warning (or error so it gets seen?), but write success back to producer.
                    log.error("Unexpected HTTP status {}", status);
                    successfullyPublished();
                }

                @Override
                public void clientError(final Problem problem) {
                    log.error("Some client error occurred (not a validation problem): {}", problem);
                    // TODO analyze problem, react accordingly.
                }
            });
    }

}
