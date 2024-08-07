package br.com.labbs.quarkusmonitor.reactive.core;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import jakarta.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.config.ConfigProvider;

public class Metrics {

  private static final String TYPE = "type";
  private static final String STATUS = "status";
  private static final String METHOD = "method";
  private static final String ADDR = "addr";
  private static final String IS_ERROR = "isError";
  private static final String ERROR_MESSAGE = "errorMessage";
  private static final String NAME = "name";
  private static final String VERSION = "version";

  private static final List<String> tagsKeysRequest = Arrays
      .asList(TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE);
  private static final List<String> tagsKeysDependency = Arrays
      .asList(NAME, TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE);

  private static final MeterRegistry registry = CDI.current().select(MeterRegistry.class).get();
  private static final ConcurrentMap<String, AtomicInteger> dependencyGaugeMap = new ConcurrentHashMap<>();
  private static final ConcurrentMap<String, AtomicLong> responseSizeGaugeMap = new ConcurrentHashMap<>();
  private static final AtomicInteger appInfoGaugeMap = new AtomicInteger(0);

  private static final double[] bucketsValues = Arrays.stream(
      ConfigProvider.getConfig().getOptionalValue("quarkus.b5.monitor.buckets", String.class)
          .orElse("0.1, 0.3, 1.5, 10.5").split(","))
      .map(String::trim).mapToDouble(Double::parseDouble).toArray();

  public static final String APP_INFO = "application_info";
  public static final String RESPONSE_SIZE = "response_size";
  public static final String DEPENDENCY_REQUEST = "dependency_request";
  public static final String REQUEST = "request";
  public static final String DEPENDENCY_UP = "dependency_up";

  private static final BiFunction<double[], TemporalUnit, Duration[]> durationBucketDefaultFunction =
      (values, unit) -> Arrays.stream(values).mapToObj(v -> Duration.of(secondsToMilliseconds(v), unit)).toArray(Duration[]::new);

  private Metrics() {
  }

  /**
   * Create a dependency request metric in seconds with name dependency_request_seconds and tag values for the following tag keys NAME,
   * TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE
   *
   * @param tagsValues values in order for tag keys NAME, TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE
   * @param seconds how long time did the dependency request has executed
   */
  public static void dependencyRequestSeconds(String[] tagsValues, double seconds) {
    dependencyRequestSeconds(tagsValues, seconds, bucketsValues, ChronoUnit.MILLIS);
  }

  /**
   * Create a dependency request metric in seconds with name dependency_request_seconds and tag values for the following tag keys NAME,
   * TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE
   *
   * @param tagsValues values in order for tag keys NAME, TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE.
   * @param seconds how long time did the dependency request has executed.
   * @param bucketList array of double values for bucket
   * @param timeUnit Unit of time in Milis, seconds for metric and buckets.
   */
  public static void dependencyRequestSeconds(String[] tagsValues, double seconds, double[] bucketList, TemporalUnit timeUnit) {
    createTimer(DEPENDENCY_REQUEST,
        "records in a histogram the number of requests of a dependency and their duration in seconds",
        tagWithValue(tagsKeysDependency, tagsValues),
        secondsToMilliseconds(seconds), timeUnit,
        Arrays.stream(bucketList).mapToObj(v -> Duration.of(secondsToMilliseconds(v), timeUnit)).toArray(Duration[]::new));
  }

  /**
   * Create a request metric in seconds with name request_seconds and tag values for the following tag keys TYPE, STATUS, METHOD, ADDR,
   * IS_ERROR, ERROR_MESSAGE
   *
   * @param tagsValues values in order for tag keys TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE
   * @param seconds how long time did the request has executed
   */
  public static void requestSeconds(String[] tagsValues, double seconds) {
    requestSeconds(tagsValues, seconds, bucketsValues, ChronoUnit.MILLIS);
  }

  /**
   * Create a request metric in seconds with name request_seconds and tag values for the following tag keys TYPE, STATUS, METHOD, ADDR,
   * IS_ERROR, ERROR_MESSAGE
   *
   * @param tagsValues values in order for tag keys TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE
   * @param seconds how long time did the request has executed
   * @param bucketList array of double values for bucket
   * @param timeUnit Unit of time in Milis, seconds for metric and buckets.
   */
  public static void requestSeconds(String[] tagsValues, double seconds, double[] bucketList, TemporalUnit timeUnit) {
    createTimer(REQUEST,
        "records in a histogram the number of http requests and their duration in seconds",
        tagWithValue(tagsKeysRequest, tagsValues),
        secondsToMilliseconds(seconds), timeUnit,
        Arrays.stream(bucketList).mapToObj(v -> Duration.of(secondsToMilliseconds(v), timeUnit)).toArray(Duration[]::new));
  }

  private static void createTimer(String name, String description, Iterable<Tag> tags,
      long value, TemporalUnit unit, Duration[] duration) {
    Optional.ofNullable(registry.find(name).tags(tags).timer()).ifPresentOrElse(
        metric -> metric.record(Duration.of(value, unit)),
        () -> Timer.builder(name)
            .description(description)
            .tags(tags)
            .serviceLevelObjectives(duration)
            .register(registry)
            .record(Duration.of(value, unit))
    );
  }

  private static long secondsToMilliseconds(double seconds) {
    var result = seconds * 1000;
    if (result > Long.MAX_VALUE) {
      return Long.MAX_VALUE;
    }
    return (long) result;
  }

  /**
   * Create a appliction info metric to show the version of application in the tag value
   *
   * @param version version of application
   */
  public static void applicationInfo(String version) {
    var tagVersion = Collections.singletonList(Tag.of(VERSION, version));
    appInfoGaugeMap.set(1);
    if (registry.find(APP_INFO).tags(tagVersion).gauge() == null) {
      Gauge.builder(APP_INFO, appInfoGaugeMap::get)
          .description("holds static info of an application, such as it's semantic version number")
          .tags(tagVersion)
          .register(registry);
    }
  }

  /**
   * Create a response size metric in bytes with name response_size_bytes and tag values for the following tag keys TYPE, STATUS, METHOD,
   * ADDR, IS_ERROR, ERROR_MESSAGE
   *
   * @param tagsValues values in order for tag keys TYPE, STATUS, METHOD, ADDR, IS_ERROR, ERROR_MESSAGE
   * @param size size of response in bytes.
   */
  public static void responseSizeBytes(String[] tagsValues, double size) {
    var description = "is a counter that computes how much data is being sent back to the user for a given request type. "
        + "It captures the response size from the content-length response header. If there is no such header, "
        + "the value exposed as metric will be zero";
    var tags = tagWithValue(tagsKeysRequest, tagsValues);
    var keyTag = tags.stream().map(Tag::getValue).collect(Collectors.joining("#"));

    if (registry.find(RESPONSE_SIZE).tags(tags).gauge() == null) {
      var value = new AtomicLong(0);
      responseSizeGaugeMap.put(keyTag, value);
      Gauge.builder(RESPONSE_SIZE, value::get)
          .description(description)
          .tags(tags)
          .baseUnit("bytes")
          .register(registry);
    }

    responseSizeGaugeMap.computeIfPresent(keyTag, (key, value) -> {
      value.addAndGet(((Double) size).longValue());
      return value;
    });
    responseSizeGaugeMap.computeIfAbsent(keyTag, key -> new AtomicLong(0));
  }

  /**
   * Create a metric with name dependency_up to show if a dependency is up
   *
   * @param dependencyName name of dependency in tag value of metric
   */
  public static void dependencyUp(String dependencyName) {
    createGaugeDependency(Tag.of(NAME, dependencyName));
    dependencyGaugeMap.computeIfPresent(dependencyName, (key, value) -> {
      value.set(1);
      return value;
    });
    dependencyGaugeMap.computeIfAbsent(dependencyName, key -> new AtomicInteger(1));
  }

  /**
   * Create a metric with name dependency_up to show if a dependency is down
   *
   * @param dependencyName name of dependency in tag value of metric
   */
  public static void dependencyDown(String dependencyName) {
    createGaugeDependency(Tag.of(NAME, dependencyName));
    dependencyGaugeMap.computeIfPresent(dependencyName, (key, value) -> {
      value.set(0);
      return value;
    });
    dependencyGaugeMap.computeIfAbsent(dependencyName, key -> new AtomicInteger(0));
  }

  private static void createGaugeDependency(Tag tag) {
    if (registry.find(DEPENDENCY_UP).tags(Collections.singletonList(tag)).gauge() == null) {
      var value = new AtomicInteger(0);
      dependencyGaugeMap.put(tag.getValue(), value);
      Gauge.builder(DEPENDENCY_UP, value::get)
          .description("is a metric to register weather a specific dependency is up (1) or down (0). "
              + "The label name registers the dependency name")
          .tags(Collections.singletonList(tag))
          .register(registry);
    }
  }

  private static Collection<Tag> tagWithValue(List<String> tagsKeys, String[] tagsValues) {
    var tagList = new ArrayList<Tag>();

    for (int i = 0; i < tagsKeys.size(); i++) {
      if (i < tagsValues.length) {
        tagList.add(Tag.of(tagsKeys.get(i), tagsValues[i]));
      } else {
        tagList.add(Tag.of(tagsKeys.get(i), ""));
      }
    }

    return tagList;
  }
}
