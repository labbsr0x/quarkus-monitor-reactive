package br.com.labbs.quarkusmonitor.reactive.filter;

import java.time.Instant;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class MetricsClientRequestFilter implements ClientRequestFilter {
    private static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS_CLIENT";

    @Override
    public void filter(ClientRequestContext clientRequestContext) {
        clientRequestContext.setProperty(TIMER_INIT_TIME_MILLISECONDS, Instant.now());
    }

}
