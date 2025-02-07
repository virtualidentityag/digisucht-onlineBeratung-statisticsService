package de.caritas.cob.statisticsservice.config.apiclient;

import de.caritas.cob.statisticsservice.api.exception.httpresponses.InternalServerErrorException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class TenantServiceHelper {

  private static final String FILTER_NAME = "filter";

  private TenantServiceHelper() { // hide public constructor
  }

  public static boolean noValidFilterParams(String queryName, Object queryValue) {
    return isEmpty(queryName) || !queryName.equals(FILTER_NAME) || isNull(queryValue);
  }

  public static MultiValueMap<String, String> obtainQueryParameters(Object queryValue) {
    MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();

    try {
      Arrays.asList(
              Introspector.getBeanInfo(queryValue.getClass(), Object.class)
                  .getPropertyDescriptors())
          .stream()
          .filter(descriptor -> nonNull(descriptor.getReadMethod()))
          .forEach(descriptor -> setMethodKeyValuePairs(queryValue, paramMap, descriptor));
      return paramMap;

    } catch (IntrospectionException exception) {
      throw new InternalServerErrorException(
          "Could not obtain method properties of %s".formatted(queryValue.toString()),
          exception);
    }
  }

  private static void setMethodKeyValuePairs(
      Object queryValue, MultiValueMap<String, String> map, PropertyDescriptor descriptor) {
    try {
      Object value = descriptor.getReadMethod().invoke(queryValue);
      if (nonNull(value)) {
        map.add(descriptor.getName(), value.toString());
      }
    } catch (Exception exception) {
      throw new InternalServerErrorException(
          "Could not obtain method key value pairs of %s".formatted(queryValue.toString()),
          exception);
    }
  }
}
