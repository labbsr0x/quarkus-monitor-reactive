package br.com.labbs.quarkusmonitor.reactive.filter;

import java.time.Instant;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import br.com.labbs.quarkusmonitor.reactive.MonitorMetrics;
import br.com.labbs.quarkusmonitor.reactive.core.Metrics;
import br.com.labbs.quarkusmonitor.reactive.util.FilterUtils;
import br.com.labbs.quarkusmonitor.reactive.util.TagsUtil;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class MetricsClientResponseFilter implements ClientResponseFilter {
	
    private static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS_CLIENT";
    
    Logger log = Logger.getLogger(MetricsClientResponseFilter.class);

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) {
        var labels = TagsUtil.extractLabelValues(clientRequestContext, clientResponseContext);
        var tagNameValue = FilterUtils.extractClassNameFromMethod(clientRequestContext);
        if (clientResponseContext.getStatus() >= 200 && clientResponseContext.getStatus() < 500) {
            Metrics.dependencyUp(tagNameValue);
        } else if (clientResponseContext.getStatus() >= 500) {
            Metrics.dependencyDown(tagNameValue);
        }
        if (clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS) != null) {
            Instant init = (Instant) clientRequestContext.getProperty(TIMER_INIT_TIME_MILLISECONDS);
            Metrics.dependencyRequestSeconds(labels,
                MonitorMetrics.calcTimeElapsedInSeconds(init));
        }
    }
}
