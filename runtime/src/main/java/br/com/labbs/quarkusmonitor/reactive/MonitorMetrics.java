package br.com.labbs.quarkusmonitor.reactive;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.labbs.quarkusmonitor.reactive.core.Metrics;
import br.com.labbs.quarkusmonitor.reactive.model.DependencyEvent;
import br.com.labbs.quarkusmonitor.reactive.model.DependencyState;
import br.com.labbs.quarkusmonitor.reactive.model.RequestEvent;

public class MonitorMetrics {

  private static final Logger LOG = LoggerFactory.getLogger(MonitorMetrics.class);
  public static final MonitorMetrics INSTANCE = new MonitorMetrics();
  private static final BigDecimal MULTIPLIER_NANO_TO_SECONDS = BigDecimal.valueOf(1.0E9D);
  private final Map<String, ScheduledExecutorService> schedulesCheckers;

  private MonitorMetrics() {
    schedulesCheckers = new HashMap<>();
  }

  /**
   * Add dependency to be checked successive between the period
   *
   * @param name name of dependency checker
   * @param task task for dependency checker
   * @param time time in unit between successive task executions
   * @param unit unit of time for task executions
   */
  public void addDependencyChecker(String name, Supplier<DependencyState> task, long time,
      TimeUnit unit) {
    if (schedulesCheckers.containsKey(name)) {
      cancelDependencyChecker(name);
    }

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    executor.scheduleWithFixedDelay(() -> {
      if (DependencyState.UP.equals(task.get())) {
        LOG.debug("Checker: {} is UP", name);
        Metrics.dependencyUp(name);
      } else {
        LOG.debug("Checker: {} is DOWN", name);
        Metrics.dependencyDown(name);
      }
    }, time, time, unit);

    schedulesCheckers.put(name, executor);
  }

  /**
   * Cancel all scheduled dependency checkers and terminates the executor timer.
   */
  public void cancelAllDependencyCheckers() {
    var listOfKeys = new HashSet<>(schedulesCheckers.keySet());
    listOfKeys.forEach(this::cancelDependencyChecker);
  }

  /**
   * Cancel the scheduled dependency checker and terminates the executor timer.
   *
   * @param name dependency checker
   */
  public void cancelDependencyChecker(String name) {
    ScheduledExecutorService executor = schedulesCheckers.get(name);
    try {
      LOG.debug("attempt to shutdown executor {}", name);
      executor.shutdown();
      if (executor.awaitTermination(1, TimeUnit.SECONDS)){
        LOG.debug("tasks shutdown in executor {} successfully.", name);
      } else {
        LOG.debug("tasks interrupted in executor {}", name);
      }
    } catch (InterruptedException e) {
      LOG.debug("tasks interrupted in executor {}", name);
    } finally {
      if (!executor.isTerminated()) {
        LOG.debug("cancel non-finished tasks in executor {}", name);
      }
      executor.shutdownNow();
      LOG.debug("shutdown finished in executor {}", name);
      schedulesCheckers.remove(name);
    }
  }

  private final Function<DependencyEvent, String[]> dependencyEventFunction = event -> new String[]{
      event.getName(),
      event.getType(),
      event.getStatus(),
      event.getMethod(),
      event.getAddress(),
      event.getIsError(),
      event.getErrorMessage()};

  private final Function<RequestEvent, String[]> requestEventFunction = event -> new String[]{
      event.getType(),
      event.getStatus(),
      event.getMethod(),
      event.getAddress(),
      event.getIsError(),
      event.getErrorMessage()};

  /**
   * Add a dependency event to be monitored with elapsed time
   *
   * @param event properties of event to be monitored
   * @param elapsedSeconds time in seconds to be register in metric
   */
  public void addDependencyEvent(DependencyEvent event, double elapsedSeconds) {
    Metrics.dependencyRequestSeconds(dependencyEventFunction.apply(event), elapsedSeconds);
  }

  /**
   * Add a dependency event to be monitored with elapsed time with bucket list using ChronoUnit.MILLIS as timeunit
   * @param event properties of event to be monitored
   * @param elapsedSeconds time in seconds to be register in metric
   * @param bucketList array of double for bucket list in Dependency event
   */
  public void addDependencyEvent(DependencyEvent event, double elapsedSeconds, double[] bucketList) {
    addDependencyEvent(event, elapsedSeconds, bucketList, ChronoUnit.MILLIS);
  }

  /**
   * Add a dependency event to be monitored with elapsed time
   *
   * @param event properties of event to be monitored
   * @param elapsedSeconds time in seconds to be register in metric
   * @param bucketList array of double for bucket list in Dependency event
   * @param metricUnit time unit for bucket list and metric
   */
  public void addDependencyEvent(DependencyEvent event, double elapsedSeconds, double[] bucketList, TemporalUnit metricUnit) {
    Metrics.dependencyRequestSeconds(dependencyEventFunction.apply(event), elapsedSeconds, bucketList, metricUnit);
  }

  /**
   * Get all checkers in execution
   *
   * @return Collection of checkers in execution
   */
  public Collection<String> listOfCheckersScheduled() {
    return Collections.unmodifiableSet(schedulesCheckers.keySet());
  }

  /**
   * Add a request event to be monitored with elapsed time
   *
   * @param event properties of event to be monitored
   * @param elapsedSeconds time in seconds to be register in metric
   */
  public void addRequestEvent(RequestEvent event, double elapsedSeconds) {
    Metrics.requestSeconds(requestEventFunction.apply(event), elapsedSeconds);
  }

  /**
   * Add a request event to be monitored with elapsed time using ChronoUnit.MILLIS as timeunit
   *
   * @param event properties of event to be monitored
   * @param elapsedSeconds time in seconds to be register in metric
   * @param bucketList array of double for bucket list in Dependency event
   */
  public void addRequestEvent(RequestEvent event, double elapsedSeconds, double[] bucketList) {
    addRequestEvent(event, elapsedSeconds, bucketList, ChronoUnit.MILLIS);
  }

  /**
   * Add a request event to be monitored with elapsed time using ChronoUnit.MILLIS as timeunit
   *
   * @param event properties of event to be monitored
   * @param elapsedSeconds time in seconds to be register in metric
   * @param bucketList array of double for bucket list in Dependency event
   * @param metricUnit time unit for bucket list and metric
   */
  public void addRequestEvent(RequestEvent event, double elapsedSeconds, double[] bucketList, TemporalUnit metricUnit) {
    Metrics.requestSeconds(requestEventFunction.apply(event), elapsedSeconds, bucketList, metricUnit);
  }

  /**
   * Calculate the elapsed time in seconds
   *
   * @param init initial time
   * @return time in seconds
   */
  public static double calcTimeElapsedInSeconds(Instant init) {
    var finish = Instant.now();
    BigDecimal diff = new BigDecimal(Duration.between(init, finish).toNanos());
    return diff.divide(MULTIPLIER_NANO_TO_SECONDS, 9, RoundingMode.HALF_UP).doubleValue();
  }

}