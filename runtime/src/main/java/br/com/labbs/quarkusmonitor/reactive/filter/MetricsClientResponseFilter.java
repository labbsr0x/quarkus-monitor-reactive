package br.com.labbs.quarkusmonitor.reactive.filter;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import br.com.labbs.quarkusmonitor.reactive.MonitorMetrics;
import br.com.labbs.quarkusmonitor.reactive.core.Metrics;
import br.com.labbs.quarkusmonitor.reactive.util.FilterUtils;
import br.com.labbs.quarkusmonitor.reactive.util.TagsUtil;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.client.impl.ClientRequestContextImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class MetricsClientResponseFilter implements ClientResponseFilter {

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) {
        var labels = retrieveLabelsFromContext(clientRequestContext, clientResponseContext);
        var tagNameValue = FilterUtils.extractClassNameFromMethod(clientRequestContext);
        if (clientResponseContext.getStatus() >= 200 && clientResponseContext.getStatus() < 500) {
            Metrics.dependencyUp(tagNameValue);
        } else if (clientResponseContext.getStatus() >= 500) {
            Metrics.dependencyDown(tagNameValue);
        }
        if (clientRequestContext.getProperty(FilterUtils.TIMER_INIT_TIME_MILLISECONDS_CLIENT) != null) {
            Instant init = (Instant) clientRequestContext.getProperty(FilterUtils.TIMER_INIT_TIME_MILLISECONDS_CLIENT);
            Metrics.dependencyRequestSeconds(labels,
                MonitorMetrics.calcTimeElapsedInSeconds(init));
        }
    }

    public String[] retrieveLabelsFromContext(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) {
        var annotation = getAnnotation(clientRequestContext);

        if(!annotation.name().isBlank() && annotation.address().isBlank()){
            return TagsUtil.extractLabelValues(annotation.name(), clientRequestContext, clientResponseContext);
        }
        if(annotation.name().isBlank() && !annotation.address().isBlank()){
            return TagsUtil.extractLabelValues(clientRequestContext, clientResponseContext, annotation.address());
        }

        if(!annotation.name().isBlank() && !annotation.address().isBlank()){
            return TagsUtil.extractLabelValues(annotation.name(),
                    clientRequestContext, clientResponseContext, annotation.address());
        }

        return TagsUtil.extractLabelValues(clientRequestContext, clientResponseContext);
    }

    private TagValuesRestClient getAnnotation(ClientRequestContext clientRequestContext) {
        return Optional.of(clientRequestContext)
                .filter(ClientRequestContextImpl.class::isInstance)
                .map(ClientRequestContextImpl.class::cast)
                .map(ClientRequestContextImpl::getRestClientRequestContext)
                .map(RestClientRequestContext::getInvokedMethod)
                .map(m -> m.getAnnotation(TagValuesRestClient.class))
                .orElse(new TagValuesRestClient() {
                    @Override
                    public int hashCode() {
                        return super.hashCode();
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return TagValuesRestClient.class;
                    }

                    @Override
                    public String name() {
                        return "";
                    }

                    @Override
                    public String address() {
                        return "";
                    }
                });
    }

}
