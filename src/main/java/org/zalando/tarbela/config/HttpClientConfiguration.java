package org.zalando.tarbela.config;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.httpclient.LogbookHttpRequestInterceptor;
import org.zalando.logbook.httpclient.LogbookHttpResponseInterceptor;
import org.zalando.tracer.Tracer;
import org.zalando.tracer.httpclient.TracerHttpRequestInterceptor;

@Configuration
@AutoConfigureAfter({ Logbook.class })
public class HttpClientConfiguration {

    @Bean
    public HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory(final Tracer tracer, final Logbook logbook) {

        final HttpClient httpClient = HttpClientBuilder.create().addInterceptorFirst(new TracerHttpRequestInterceptor(tracer))
                                                 .addInterceptorLast(new LogbookHttpRequestInterceptor(logbook))
                                                 .addInterceptorLast(new LogbookHttpResponseInterceptor()).build();

        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);

        return factory;
    }
}
