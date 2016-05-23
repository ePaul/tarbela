package org.zalando.tarbela.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Workaround for enabling type mapping from EventPayload to HashMap in swagger-codegen-maven-plugin.
 */
public class EventPayload extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new empty EventPayload.
     */
    public EventPayload() { }

    /**
     * Creates an EventPayload pre-filled with the given mappings.
     */
    public EventPayload(final Map<? extends String, ? extends Object> m) {
        super(m);
    }

}
