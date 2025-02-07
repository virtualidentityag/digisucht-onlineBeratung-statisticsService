package de.caritas.cob.statisticsservice.api.exception.httpresponses;

import de.caritas.cob.statisticsservice.api.service.LogService;

import java.io.Serial;

public class BadRequestException extends CustomHttpStatusException {

  @Serial
  private static final long serialVersionUID = -8047408802295905803L;

  /**
   * BadRequest exception.
   *
   * @param message an additional message
   */
  public BadRequestException(String message) {
    super(message, LogService::logWarning);
  }
}
