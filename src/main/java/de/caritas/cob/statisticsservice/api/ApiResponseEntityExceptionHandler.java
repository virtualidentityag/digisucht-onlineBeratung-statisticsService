package de.caritas.cob.statisticsservice.api;

import com.mongodb.MongoException;
import de.caritas.cob.statisticsservice.api.exception.KeycloakException;
import de.caritas.cob.statisticsservice.api.exception.httpresponses.BadRequestException;
import de.caritas.cob.statisticsservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.statisticsservice.api.exception.httpresponses.NotFoundException;
import de.caritas.cob.statisticsservice.api.exception.httpresponses.StatisticsDisabledException;
import de.caritas.cob.statisticsservice.api.service.LogService;
import java.net.ConnectException;
import java.net.UnknownHostException;
import jakarta.validation.ConstraintViolationException;
import lombok.NoArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Customizes API error/exception handling to hide information and/or possible security
 * vulnerabilities.
 */
@NoArgsConstructor
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Exception EMPTY_EXCEPTION = new RuntimeException();

  /**
   * Custom BadRequest exception.
   *
   * @param ex      the thrown exception
   * @param request web request
   * @return response entity
   */
  @ExceptionHandler({BadRequestException.class})
  public ResponseEntity<Object> handleCustomBadRequest(
      final BadRequestException ex, final WebRequest request) {
    LogService.logWarning(ex);

    return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({StatisticsDisabledException.class})
  public ResponseEntity<Object> handleFeatureDisabledException(
      final BadRequestException ex, final WebRequest request) {
    LogService.logWarning(ex);

    return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
  }

  /**
   * Constraint violations.
   *
   * @param ex      the thrown exception
   * @param request web request
   * @return response entity
   */
  @ExceptionHandler({ConstraintViolationException.class})
  public ResponseEntity<Object> handleBadRequest(
      final ConstraintViolationException ex, final WebRequest request) {
    LogService.logWarning(ex);

    return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
  }

  /**
   * Incoming request body could not be deserialized.
   *
   * @param ex      the thrown exception
   * @param headers http headers
   * @param status  http status
   * @param request web request
   * @return response entity
   */
  @NonNull
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      final @NonNull HttpMessageNotReadableException ex,
      final @NonNull HttpHeaders headers,
      final HttpStatusCode status,
      final @NonNull WebRequest request) {
    LogService.logWarning(HttpStatus.valueOf(status.value()), ex);

    return new ResponseEntity<>(null, status);
  }

  /**
   * Valid on object fails validation.
   *
   * @param ex      the thrown exception
   * @param headers http headers
   * @param status  http status
   * @param request web request
   * @return response entity
   */
  @NonNull
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      final @NonNull MethodArgumentNotValidException ex,
      final @NonNull HttpHeaders headers,
      final HttpStatusCode status,
      final @NonNull WebRequest request) {
    LogService.logWarning(HttpStatus.valueOf(status.value()), ex);

    return new ResponseEntity<>(null, status);
  }

  /**
   * 409 - Conflict.
   *
   * @param ex      the thrown exception
   * @param request web request
   * @return response entity
   */
  @ExceptionHandler({InvalidDataAccessApiUsageException.class})
  protected ResponseEntity<Object> handleConflict(
      final RuntimeException ex, final WebRequest request) {
    LogService.logWarning(HttpStatus.CONFLICT, ex);

    return new ResponseEntity<>(null, HttpStatus.CONFLICT);
  }

  /**
   * 500 - Internal Server Error.
   *
   * @param ex      the thrown exception
   * @param request web request
   * @return response entity
   */
  @ExceptionHandler({NullPointerException.class, IllegalArgumentException.class,
      IllegalStateException.class, KeycloakException.class,
      UnknownHostException.class, DataAccessException.class,
      ConnectException.class, MongoException.class})
  public ResponseEntity<Object> handleInternal(
      final RuntimeException ex, final WebRequest request) {
    LogService.logInternalServerError(ex);

    return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * 500 - Internal Server Error with custom logging method.
   *
   * @param ex      the thrown exception
   * @param request web request
   * @return response entity
   */
  @ExceptionHandler({InternalServerErrorException.class})
  public ResponseEntity<Object> handleInternal(
      final InternalServerErrorException ex, final WebRequest request) {
    ex.executeLogging();

    return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * 404 - Not found.
   *
   * @param ex      {@link NotFoundException}
   * @param request WebRequest
   * @return a ResponseEntity instance
   */
  @ExceptionHandler({NotFoundException.class})
  public ResponseEntity<Object> handleInternal(
      final NotFoundException ex, final WebRequest request) {

    return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
  }

  /**
   * Handles generic HTTP client error status for generated apis.
   *
   * @param ex      {@link HttpClientErrorException}
   * @param request {@link WebRequest}
   * @return response entity
   */
  @ExceptionHandler({HttpClientErrorException.class})
  public ResponseEntity<Object> handleInternal(final HttpClientErrorException ex,
      final WebRequest request) {
    LogService.logError(ex);

    return new ResponseEntity<>(null, ex.getStatusCode());
  }

}
