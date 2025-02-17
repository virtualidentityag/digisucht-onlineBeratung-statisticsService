package de.caritas.cob.statisticsservice.api.exception.httpresponses;

import de.caritas.cob.statisticsservice.api.service.LogService;

import java.io.Serial;
import java.util.function.Consumer;

/**
 * Represents the exception for an internal server error - status code 500.
 */
public class InternalServerErrorException extends CustomHttpStatusException {

  @Serial
  private static final long serialVersionUID = 6051508644381775936L;

  /**
   * InternalServerErrorException exception.
   *
   * @param message the message to be logged
   */
  public InternalServerErrorException(String message) {
    super(message, LogService::logInternalServerError);
  }

  /**
   * InternalServerErrorException exception.
   *
   * @param loggingMethod the method to be used for logging
   * @param message       the message to be logged
   */
  public InternalServerErrorException(Consumer<Exception> loggingMethod, String message) {
    super(message, loggingMethod);
  }

  /**
   * InternalServerError exception.
   *
   * @param message the exception message
   * @param ex      the exception
   */
  public InternalServerErrorException(String message, Exception ex) {
    super(message, ex, LogService::logInternalServerError);
  }
}
