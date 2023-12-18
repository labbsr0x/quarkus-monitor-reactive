package br.com.labbs.quarkusmonitor.reactive.filter;

import java.io.IOException;
import java.time.Instant;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.ext.Provider;

import br.com.labbs.quarkusmonitor.reactive.util.FilterUtils;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class MetricsServiceRequestFilter implements ContainerRequestFilter {
	
	@Inject
	ResourceInfo resourceInfo;
	
	@Override
	public void filter(ContainerRequestContext request) throws IOException {
		var pathWithId = FilterUtils.toPathWithParamId(request, resourceInfo);
		var isValid = FilterUtils.validPath(pathWithId);

		request.setProperty(FilterUtils.VALID_PATH_FOR_METRICS, isValid);

		if (isValid) {
			request.setProperty(FilterUtils.PATH_WITH_PARAM_ID, pathWithId);
			request.setProperty(FilterUtils.TIMER_INIT_TIME_MILLISECONDS, Instant.now());
		}
	}

}
