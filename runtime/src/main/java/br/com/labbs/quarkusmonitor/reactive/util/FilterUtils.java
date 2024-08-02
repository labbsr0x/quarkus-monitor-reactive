package br.com.labbs.quarkusmonitor.reactive.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Named;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.eclipse.microprofile.config.ConfigProvider;

import io.micrometer.core.instrument.config.NamingConvention;
import org.jboss.resteasy.reactive.client.impl.ClientRequestContextImpl;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.ContainerRequestContextImpl;

public class FilterUtils {

  private static final Pattern tagKeyChars = Pattern.compile("[^a-zA-Z0-9_]");

  public static final String TIMER_INIT_TIME_MILLISECONDS = "TIMER_INIT_TIME_MILLISECONDS";
  public static final String STATUS_CODE = "STATUS_CODE";
  public static final String VALID_PATH_FOR_METRICS = "VALID_PATH_FOR_METRICS";
  public static final String PATH_WITH_PARAM_ID = "PATH_WITH_PARAM_ID";

  private static final Collection<String> exclusions =
      Arrays.stream(
          ConfigProvider.getConfig()
              .getOptionalValue("quarkus.b5.monitor.exclusions", String.class)
              .orElse("")
              .split(","))
          .map(Object::toString)
          .map(String::trim)
          .toList();
  private static final String REST_CLIENT_METHOD = "org.eclipse.microprofile.rest.client.invokedMethod";

  private FilterUtils() {
  }

  public static boolean validPath(String pathWithParamId) {
    return exclusions.stream().noneMatch(path -> path.equalsIgnoreCase(pathWithParamId));
  }

  public static Integer extractStatusCodeFromContext(WriterInterceptorContext context) {
    return Integer.valueOf(context.getProperty(STATUS_CODE).toString());
  }

  public static String extractClassNameFromMethod(ClientRequestContext request) {
    if (request.getProperty(REST_CLIENT_METHOD) instanceof Method method) {
        var annotation = method.getDeclaringClass().getAnnotation(Named.class);

      if (annotation != null && !annotation.value().isBlank()) {
        return tagConvert(annotation.value());
      }

      return method.getDeclaringClass().getCanonicalName();
    }

    return "";
  }

  public static String toPathWithParamId(ClientRequestContext request) {
    if (request.getProperty(REST_CLIENT_METHOD) instanceof Method method) {
      return extractPathWithParamFromMethod(method, request.getUri().getPath());
    }

    return extractPathWithParamFromMethod(null, request.getUri().getPath());
  }

  public static String toPathWithParamId(ContainerRequestContext request){
    if(request instanceof ContainerRequestContextImpl containerRequestContext
        && containerRequestContext.getServerRequestContext() instanceof  ResteasyReactiveRequestContext reactiveRequestContext
        && reactiveRequestContext.getTarget() != null
        && reactiveRequestContext.getTarget().getPath() != null) {
      return reactiveRequestContext.getTarget().getPath().template;
    }
    return request.getUriInfo().getPath();
  }


  private static String extractPathWithParamFromMethod(Method method, String defaultPath) {
    String pathWithParam = "";

    if (method == null) {
      return defaultPath;
    }

    if (method.getDeclaringClass().getAnnotation(Path.class) != null) {
      pathWithParam = method.getDeclaringClass().getAnnotation(Path.class).value();
    }

    if (method.getAnnotation(Path.class) != null) {
      String methodValue = method.getAnnotation(Path.class).value();
      if (methodValue != null && !methodValue.startsWith("/")) {
    	  methodValue = "/" + methodValue;
      }
      pathWithParam = ("/".equals(pathWithParam)? "": pathWithParam) + methodValue;
    }

    return pathWithParam.isEmpty() ? defaultPath : pathWithParam;
  }

  public static String tagConvert(String key) {
    String conventionKey = NamingConvention.snakeCase.tagKey(key);

    String sanitized = tagKeyChars.matcher(conventionKey).replaceAll("_");
    if (!Character.isLetter(sanitized.charAt(0))) {
      sanitized = "m_" + sanitized;
    }
    return sanitized;
  }
}
