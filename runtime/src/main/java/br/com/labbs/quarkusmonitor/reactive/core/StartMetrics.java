package br.com.labbs.quarkusmonitor.reactive.core;

import io.quarkus.runtime.StartupEvent;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class StartMetrics {

  void onStart(@Observes StartupEvent ev) {
    Metrics.applicationInfo(ConfigProvider.getConfig()
        .getOptionalValue("quarkus.application.version", String.class).orElse("not-set"));
  }
}
