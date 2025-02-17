package de.caritas.cob.statisticsservice.api.exception;

import java.io.Serial;

public class KeycloakException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -5083156826149548581L;

  /**
   * Keycloak 409 Conflict exception
   *
   * @param message
   */
  public KeycloakException(String message) {
    super(message);
  }

}
