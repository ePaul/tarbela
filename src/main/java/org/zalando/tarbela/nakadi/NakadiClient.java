package org.zalando.tarbela.nakadi;

import java.util.List;
import java.util.Map;

/**
 * The interface to the Nakadi client.
 */
public interface NakadiClient {

    /**
     * Submits some events to Nakadi. The given callback will be called when we are done.
     *
     * @param  eventType  the event type. (This determines the submission URI.)
     * @param  events     the list of events to submit. This client is ignorant about its format, they will be JSON-ized
     *                    and passed through as-is.
     * @param  callback   one of the methods of the callback will be called, depending on the response code.
     */
    void submitEvents(String eventType, List<? extends Map<String, Object>> events, NakadiResponseCallback callback);
}
