package br.com.labbs.quarkusmonitor.reactive.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "b5.monitor")
public interface MetricsB5Configuration {
    /**
     * Enable the extension.
     */
    @WithDefault("true")
    boolean enable();

    /**
     * Define the path where the metrics are exposed.
     */
    @WithDefault("/metrics")
    String path();

    /**
     * Define the path where the metrics are exposed.
     */
    @WithDefault("0.1, 0.3, 1.5, 10.5")
    String buckets();

    /**
     * Define the paths where the b5 metrics are not apply.
     */
    @WithDefault("/metrics")
    String exclusions();

    /**
     * Define to turn on or off the http response size, default false
     */
    @WithDefault("false")
    boolean enableHttpResponseSize();

    /**
     * Define the key for error messages put in the request attribute
     */
    @WithDefault("error-info")
    String errorMessage();
}
