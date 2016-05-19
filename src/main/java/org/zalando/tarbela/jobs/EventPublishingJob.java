package org.zalando.tarbela.jobs;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;

import org.zalando.tarbela.service.EventService;

@Component
public class EventPublishingJob {

    // Run the job every second minute for now.
    private static final long RATE = 1000 * 60 * 2;

    @Autowired
    private EventService service;

    @Scheduled(fixedRate = RATE)
    public void publishStuff() {
        service.publishEvents();
    }

}
