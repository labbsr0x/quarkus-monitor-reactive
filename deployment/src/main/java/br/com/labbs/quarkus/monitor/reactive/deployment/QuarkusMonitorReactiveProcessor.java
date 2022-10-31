package br.com.labbs.quarkus.monitor.reactive.deployment;

import br.com.labbs.quarkusmonitor.reactive.config.MetricsB5Configuration;
import br.com.labbs.quarkusmonitor.reactive.core.StartMetrics;
import br.com.labbs.quarkusmonitor.reactive.filter.MetricsClientFilter;
import br.com.labbs.quarkusmonitor.reactive.filter.MetricsServiceFilter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;

class QuarkusMonitorReactiveProcessor {

    private static final String FEATURE = "quarkus-monitor-reactive";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
    
    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
      return new AdditionalBeanBuildItem.Builder()
          .setUnremovable()
          .addBeanClass(StartMetrics.class)
          .build();
    }

    @BuildStep
    void addProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
        MetricsB5Configuration configuration) {
      if (configuration.enable) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsServiceFilter.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(MetricsClientFilter.class.getName()));
      }
    }
}
