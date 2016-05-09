package org.zalando.tarbela.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.tracer.FlowIDGenerator;
import org.zalando.tracer.MDCTraceListener;
import org.zalando.tracer.Trace;
import org.zalando.tracer.Tracer;

@Configuration
@ConditionalOnClass(Tracer.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class TraceConfiguration {

    public static final String FILTER_NAME = "tracerFilter";
    public static final String TRACE_ID = "X-FLOW-ID";

    @Bean
    public Tracer tracer() {
        return
            Tracer.builder()                              //
                  .trace(TRACE_ID, new FlowIDGenerator()) //
                  .listener(new MDCTraceListener())       //
                  .build();
    }

    @Bean
    public Trace flowId(final Tracer tracer) {
        return tracer.get(TRACE_ID);
    }

}
