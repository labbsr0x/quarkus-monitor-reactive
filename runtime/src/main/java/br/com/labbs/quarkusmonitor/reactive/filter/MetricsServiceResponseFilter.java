package br.com.labbs.quarkusmonitor.reactive.filter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import br.com.labbs.quarkusmonitor.reactive.MonitorMetrics;
import br.com.labbs.quarkusmonitor.reactive.core.Metrics;
import br.com.labbs.quarkusmonitor.reactive.util.FilterUtils;
import br.com.labbs.quarkusmonitor.reactive.util.TagsUtil;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class MetricsServiceResponseFilter implements ContainerResponseFilter {
	
	@Override
	public void filter(ContainerRequestContext containerRequestContext,
			ContainerResponseContext containerResponseContext) throws IOException {
		if (getValidPathFromRequest(containerRequestContext)) {
			var labels = TagsUtil.extractLabelValues(containerRequestContext, containerResponseContext);

			// Foi a forma que achei para passar o status code no aroundWriteTo
			containerRequestContext.setProperty(FilterUtils.STATUS_CODE, containerResponseContext.getStatus());

			if (containerRequestContext.getProperty(FilterUtils.TIMER_INIT_TIME_MILLISECONDS) != null) {
				Instant init = (Instant) containerRequestContext.getProperty(FilterUtils.TIMER_INIT_TIME_MILLISECONDS);

				Metrics.requestSeconds(labels, MonitorMetrics.INSTANCE.calcTimeElapsedInSeconds(init));
			}
		}
	}

	private boolean getValidPathFromRequest(ContainerRequestContext request) {
		return Boolean.valueOf(
				Optional.ofNullable(request.getProperty(FilterUtils.VALID_PATH_FOR_METRICS)).orElse("").toString());
	}
}
