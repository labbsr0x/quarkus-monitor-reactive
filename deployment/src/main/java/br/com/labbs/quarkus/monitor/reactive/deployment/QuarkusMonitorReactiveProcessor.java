package br.com.labbs.quarkus.monitor.reactive.deployment;

import java.util.function.BooleanSupplier;

import br.com.labbs.quarkusmonitor.reactive.config.MetricsB5Configuration;
import br.com.labbs.quarkusmonitor.reactive.core.StartMetrics;
import br.com.labbs.quarkusmonitor.reactive.filter.MetricsClientRequestFilter;
import br.com.labbs.quarkusmonitor.reactive.filter.MetricsClientResponseFilter;
import br.com.labbs.quarkusmonitor.reactive.filter.MetricsServiceRequestFilter;
import br.com.labbs.quarkusmonitor.reactive.filter.MetricsServiceResponseFilter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;

@BuildSteps(onlyIf = QuarkusMonitorReactiveProcessor.IsEnabled.class)
class QuarkusMonitorReactiveProcessor {

	private static final String FEATURE = "quarkus-monitor-reactive";

	@BuildStep
	FeatureBuildItem feature() {
		return new FeatureBuildItem(FEATURE);
	}

	@BuildStep
	AdditionalBeanBuildItem registerAStartMetrics() {
		return new AdditionalBeanBuildItem.Builder().setUnremovable().addBeanClass(StartMetrics.class).build();
	}
	
	@BuildStep
	AdditionalBeanBuildItem registerMetricsServiceFilter() {
		return new AdditionalBeanBuildItem.Builder().setUnremovable().addBeanClass(MetricsServiceRequestFilter.class).build();
	}
	
	@BuildStep
	AdditionalBeanBuildItem registerMetricsClientFilter() {
		return new AdditionalBeanBuildItem.Builder().setUnremovable().addBeanClass(MetricsClientRequestFilter.class).build();
	}
	

	@BuildStep
	AdditionalBeanBuildItem registerMetricsServiceResponseFilter() {
		return new AdditionalBeanBuildItem.Builder().setUnremovable().addBeanClass(MetricsServiceResponseFilter.class).build();
	}
	
	@BuildStep
	AdditionalBeanBuildItem registerMetricsClientResponseFilter() {
		return new AdditionalBeanBuildItem.Builder().setUnremovable().addBeanClass(MetricsClientResponseFilter.class).build();
	}
	
	static class IsEnabled implements BooleanSupplier {
		MetricsB5Configuration configuration;

	    public boolean getAsBoolean() {
	        return configuration.enable();
	    }
	}
}
