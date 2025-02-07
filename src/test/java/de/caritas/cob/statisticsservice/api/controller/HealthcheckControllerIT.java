package de.caritas.cob.statisticsservice.api.controller;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.caritas.cob.statisticsservice.StatisticsServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = StatisticsServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@TestPropertySource(properties = "multitenancy.enabled=true")
@AutoConfigureMockMvc
@EnableAutoConfiguration
class HealthcheckControllerIT {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void getHealtcheck_Should_returnHealtcheck() throws Exception {
    mockMvc
        .perform(get("/healthcheck/health").contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("UP")));
  }

  @Test
  void getHealtcheck_Should_returnHealtcheckLiveness() throws Exception {
    mockMvc
        .perform(get("/healthcheck/health/liveness").contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("UP")));
  }

  @Test
  void getHealtcheck_Should_returnHealtcheckReadiness() throws Exception {
    mockMvc
        .perform(get("/healthcheck/health/liveness").contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status", is("UP")));
  }

}
