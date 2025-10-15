package de.caritas.cob.statisticsservice.api.controller;

import de.caritas.cob.statisticsservice.api.authorization.StatisticsFeatureAuthorisationService;
import de.caritas.cob.statisticsservice.api.model.ConsultantStatisticsResponseDTO;
import de.caritas.cob.statisticsservice.api.model.RegistrationStatisticsListResponseDTO;
import de.caritas.cob.statisticsservice.api.statistics.service.RegistrationStatisticsService;
import de.caritas.cob.statisticsservice.api.statistics.service.StatisticsService;
import de.caritas.cob.statisticsservice.generated.api.controller.StatisticsApi;
import io.swagger.annotations.Api;
import java.time.LocalDate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "statistics-controller")
@RequiredArgsConstructor
public class StatisticsController implements StatisticsApi {

  private final @NonNull StatisticsService statisticsService;

  private final @NonNull RegistrationStatisticsService registrationStatisticsService;

  private final @NonNull StatisticsFeatureAuthorisationService
      statisticsFeatureAuthorisationService;

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
        statisticsService.fetchStatisticsData(startDate, endDate), HttpStatus.OK);
  }

  /**
   * Returns registration statistics and enriches them with additional data, e.g., message count.
   *
   * @return a {@link RegistrationStatisticsListResponseDTO} instance with the statistical data.
   */
  @Override
  public ResponseEntity<RegistrationStatisticsListResponseDTO> getRegistrationStatistics() {
    statisticsFeatureAuthorisationService.assertStatisticsFeatureIsEnabled();
    return new ResponseEntity<>(
        registrationStatisticsService.fetchRegistrationStatisticsData(), HttpStatus.OK);
  }
}
