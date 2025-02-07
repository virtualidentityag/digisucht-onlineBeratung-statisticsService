package de.caritas.cob.statisticsservice.api.controller;

import de.caritas.cob.statisticsservice.api.model.HealtcheckStatus;
import de.caritas.cob.statisticsservice.api.model.HealthcheckResponseDTO;
import de.caritas.cob.statisticsservice.generated.api.controller.HealthcheckApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Controller for statistics API requests */
@RestController
@Tag(name = "healthcheck-controller")
@RequiredArgsConstructor
public class HealtcheckController implements HealthcheckApi {

  @Override
  public ResponseEntity<HealthcheckResponseDTO> getHealthcheck() {
    return new ResponseEntity<>(
        new HealthcheckResponseDTO().status(HealtcheckStatus.UP),
        HttpStatus.OK);
  }

  @Override
  public ResponseEntity<HealthcheckResponseDTO> getHealthcheckLiveness() {
    return getHealthcheck();
  }

  @Override
  public ResponseEntity<HealthcheckResponseDTO> getHealthcheckReadiness() {
    return getHealthcheck();
  }
}
