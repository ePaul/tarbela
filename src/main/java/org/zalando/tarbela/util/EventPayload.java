package org.zalando.tarbela.util;

import java.util.HashMap;

/**
 * Workaround for enabling type mapping from EventPayload to HashMap in swagger-codegen-maven-plugin.
 */
public class EventPayload extends HashMap<String, Object> { }
