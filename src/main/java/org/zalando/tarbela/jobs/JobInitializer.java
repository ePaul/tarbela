package org.zalando.tarbela.jobs;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.scheduling.TaskScheduler;

import org.zalando.tarbela.config.util.ProducerInteractor;
import org.zalando.tarbela.nakadi.NakadiClient;
import org.zalando.tarbela.service.EventServiceImpl;

import org.zalando.tracer.Tracer;

public class JobInitializer implements InitializingBean {
    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private List<ProducerInteractor> producerInteractors;

    @Autowired
    private NakadiClient nakadiClient;

    @Autowired
    private Tracer tracer;

    @Override
    public void afterPropertiesSet() throws Exception {
        producerInteractors.forEach(producerInteractor -> startJob(producerInteractor));
    }

    private void startJob(final ProducerInteractor interactor) {
        final EventServiceImpl eventService = new EventServiceImpl(interactor.getEventRetriever(),
                interactor.getEventStatusUpdater(), nakadiClient);
        taskScheduler.scheduleWithFixedDelay(() -> {
                tracer.start();
                try {
                    eventService.publishEvents();
                } finally {
                    tracer.stop();
                }
            },
            interactor.getJobInterval());
    }
}
