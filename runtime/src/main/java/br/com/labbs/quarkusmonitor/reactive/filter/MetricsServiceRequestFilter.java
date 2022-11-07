package br.com.labbs.quarkusmonitor.reactive.filter;

import java.io.IOException;
import java.time.Instant;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.ext.Provider;

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
