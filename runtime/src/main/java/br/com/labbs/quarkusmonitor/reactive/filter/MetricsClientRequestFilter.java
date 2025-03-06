package br.com.labbs.quarkusmonitor.reactive.filter;

import java.time.Instant;

import br.com.labbs.quarkusmonitor.reactive.util.FilterUtils;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class MetricsClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext clientRequestContext) {
        clientRequestContext.setProperty(FilterUtils.TIMER_INIT_TIME_MILLISECONDS_CLIENT, Instant.now());
    }

}
