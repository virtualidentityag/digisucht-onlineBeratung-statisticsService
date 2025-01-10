package de.caritas.cob.statisticsservice.api.controller;

import de.caritas.cob.statisticsservice.api.authorization.StatisticsFeatureAuthorisationService;
import de.caritas.cob.statisticsservice.api.model.ConsultantStatisticsResponseDTO;
import de.caritas.cob.statisticsservice.api.model.RegistrationStatisticsListResponseDTO;
import de.caritas.cob.statisticsservice.api.statistics.service.RegistrationStatisticsService;
import de.caritas.cob.statisticsservice.api.statistics.service.StatisticsService;
import de.caritas.cob.statisticsservice.generated.api.controller.StatisticsApi;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller for statistics API requests */
@RestController
@Tag(name = "statistics-controller")
@RequiredArgsConstructor
public class StatisticsController implements StatisticsApi {

  private final @NonNull StatisticsService statisticsService;

  private final @NonNull RegistrationStatisticsService registrationStatisticsService;

  private final @NonNull StatisticsFeatureAuthorisationService statisticsFeatureAuthorisationService;

  /**
   * Returns statistical data for a consultant.
   *
   * @param startDate start of the period (inclusive)
   * @param endDate end of the period (inclusive)
   * @return a {@link ConsultantStatisticsResponseDTO} instance with the statistical data.
   */
  @Override
  public ResponseEntity<ConsultantStatisticsResponseDTO> getConsultantStatistics(
      @RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
    statisticsFeatureAuthorisationService.assertStatisticsFeatureIsEnabled();
    return new ResponseEntity<>(
        statisticsService.fetchStatisticsData(startDate, endDate),
        HttpStatus.OK);
  }

  @Override
  public ResponseEntity<RegistrationStatisticsListResponseDTO> getRegistrationStatistics() {
    statisticsFeatureAuthorisationService.assertStatisticsFeatureIsEnabled();
    return new ResponseEntity<>(
        registrationStatisticsService.fetchRegistrationStatisticsData(),
        HttpStatus.OK);
  }
}
