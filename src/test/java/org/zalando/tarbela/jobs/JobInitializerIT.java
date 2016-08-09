package org.zalando.tarbela.jobs;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Date;

import org.junit.Test;

import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Bean;

import org.springframework.scheduling.TaskScheduler;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.zalando.tarbela.config.util.ProducerInteractor;
import org.zalando.tarbela.config.util.ProducerInteractorContainer;
import org.zalando.tarbela.nakadi.NakadiClient;
import org.zalando.tarbela.producer.EventRetriever;
import org.zalando.tarbela.producer.EventStatusUpdater;

import org.zalando.tracer.Tracer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JobInitializerIT.JobInitializerTestContext.class)
public class JobInitializerIT {
    @Autowired
    public JobInitializer jobInitializer;

    @Autowired
    public TaskScheduler taskScheduler;

    @Autowired
    public Tracer tracer;

    @Test
    public void testEnsureMultipleJobsAreSpawned() throws Exception {
        verify(taskScheduler, times(2)).scheduleWithFixedDelay(any(), any(Date.class), anyLong());
    }

    public static class JobInitializerTestContext {
        @Bean
        public TaskScheduler taskScheduler() {
            return mock(TaskScheduler.class);
        }

        @Bean
        public NakadiClient nakadiClient() {
            return mock(NakadiClient.class);
        }

        @Bean
        public Tracer tracer() {
            return mock(Tracer.class);
        }

        @Bean
        public ProducerInteractorContainer producerInteractorContainer() {
            final EventRetriever eventRetriever1 = mock(EventRetriever.class);
            final EventRetriever eventRetriever2 = mock(EventRetriever.class);

            final EventStatusUpdater eventStatusUpdater1 = mock(EventStatusUpdater.class);
            final EventStatusUpdater eventStatusUpdater2 = mock(EventStatusUpdater.class);

            ProducerInteractor producerInteractor1 = new ProducerInteractor(eventRetriever1, eventStatusUpdater2, 1,
                    "producer1");
            ProducerInteractor producerInteractor2 = new ProducerInteractor(eventRetriever2, eventStatusUpdater2, 2,
                    "producer2");

            return new ProducerInteractorContainer(newArrayList(producerInteractor1, producerInteractor2));
        }

        @Bean
        public JobInitializer jobInitializer() {
            return new JobInitializer();
        }
    }
}
