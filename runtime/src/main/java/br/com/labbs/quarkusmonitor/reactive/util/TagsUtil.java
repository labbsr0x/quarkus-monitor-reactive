package br.com.labbs.quarkusmonitor.reactive.util;

import java.util.Objects;
import java.util.Optional;

import br.com.labbs.quarkusmonitor.reactive.filter.TagValuesRestClient;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.eclipse.microprofile.config.ConfigProvider;

public class TagsUtil {

  private static final String HTTP = "http";
  private static final String ERROR_MESSAGE_KEY = ConfigProvider.getConfig()
      .getOptionalValue("quarkus.b5.monitor.error-message", String.class).orElse("error-info");

  private TagsUtil() {
  }

  public static String[] extractLabelValues(ContainerRequestContext request,
      ContainerResponseContext response) {
    var pathWithParamId = (Optional.ofNullable(request.getProperty(FilterUtils.PATH_WITH_PARAM_ID))
        .orElse(request.getUriInfo().getPath())).toString();
    return new String[]{
        HTTP,
        Integer.toString(response.getStatus()),
        request.getMethod(),
        pathWithParamId,
        Boolean.toString(isError(response.getStatus())),
        extractMessageError(request, response)
    };
  }

  public static String[] extractLabelValues(String name,ClientRequestContext request,
                                            ClientResponseContext response) {
    return extractLabelValues(name, request, response, FilterUtils.toPathWithParamId(request));
  }

  public static String[] extractLabelValues(ClientRequestContext request,
                                            ClientResponseContext response,String address) {
    return extractLabelValues(FilterUtils.extractClassNameFromMethod(request), request, response, address);
  }

  public static String[] extractLabelValues(ClientRequestContext request,
                                            ClientResponseContext response) {
    return extractLabelValues(FilterUtils.extractClassNameFromMethod(request),
            request,response,FilterUtils.toPathWithParamId(request));
  }

  public static String[] extractLabelValues(String name, ClientRequestContext request,
                                            ClientResponseContext response,String address) {
    return new String[]{
            name,
            HTTP,
            Integer.toString(response.getStatus()),
            request.getMethod(),
            address,
            Boolean.toString(isError(response.getStatus())),
            extractMessageError(request, response)};
  }

  public static String[] extractLabelValues(UriInfo uriInfo, Request request,
      WriterInterceptorContext context) {
    int statusCode = FilterUtils.extractStatusCodeFromContext(context);
    var pathWithParamId = (Optional.ofNullable(context.getProperty(FilterUtils.PATH_WITH_PARAM_ID))
        .orElse(uriInfo.getPath())).toString();
    return new String[]{
        HTTP,
        Integer.toString(statusCode),
        request.getMethod(),
        pathWithParamId,
        Boolean.toString(isError(statusCode)),
        extractMessageError(context)
    };
  }

  public static String extractMessageError(ContainerRequestContext request,
      ContainerResponseContext response) {

    if (Objects.nonNull(response.getHeaders()) && Objects.nonNull(response.getHeaders().getFirst(ERROR_MESSAGE_KEY))) {
      return response.getHeaderString(ERROR_MESSAGE_KEY);
    }

    if (request.getProperty(ERROR_MESSAGE_KEY) != null) {
      return request.getProperty(ERROR_MESSAGE_KEY).toString();
    }
    return "";
  }

  private static String extractMessageError(WriterInterceptorContext context) {
    if (Objects.nonNull(context.getHeaders()) && context.getHeaders().containsKey(ERROR_MESSAGE_KEY)) {
      return context.getHeaders().get(ERROR_MESSAGE_KEY).get(0).toString();
    }

    if (context.getProperty(ERROR_MESSAGE_KEY) != null) {
      return context.getProperty(ERROR_MESSAGE_KEY).toString();
    }
    return "";
  }

  private static String extractMessageError(ClientRequestContext request,
      ClientResponseContext response) {

    if (Objects.nonNull(response.getHeaders()) && Objects.nonNull(response.getHeaders().getFirst(ERROR_MESSAGE_KEY))) {
      return response.getHeaders().getFirst(ERROR_MESSAGE_KEY);
    }

    if (Objects.nonNull(request.getProperty(ERROR_MESSAGE_KEY))) {
      return request.getProperty(ERROR_MESSAGE_KEY).toString();
    }
    return "";
  }

  public static boolean isError(int status) {
    return status < 200 || status >= 400;
  }
}
