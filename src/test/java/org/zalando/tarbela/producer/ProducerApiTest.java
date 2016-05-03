package org.zalando.tarbela.producer;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.tarbela.TarbelaApplication;

import lombok.extern.slf4j.Slf4j;

/**
 * This test works with a running producer.
 * This won't be the case during build, so the test is ignored for now.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@SpringApplicationConfiguration(classes = TarbelaApplication.class)
@ActiveProfiles("dev")
@Ignore
public class ProducerApiTest {

    @Autowired
    private EventRetriever retriever;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testFirstPage() {
        final EventsWithNextPage events = retriever.retrieveEvents();
        Assert.assertNotNull(events);
        Assert.assertNotNull(events.getEvents());
        log.info("Retrieved events: {}", events.getEvents());
    }

    @Test
    public void testNextPage() {
        Optional<EventsWithNextPage> nextEvents = Optional.of(retriever.retrieveEvents());
        Assert.assertNotNull(nextEvents);
        while(nextEvents.isPresent()) {
            final EventsWithNextPage events = nextEvents.get();
            Assert.assertNotNull(events.getEvents());
            log.info("Retrieved events: {}", events.getEvents());
            nextEvents = events.nextPage();
        }
    }
}
