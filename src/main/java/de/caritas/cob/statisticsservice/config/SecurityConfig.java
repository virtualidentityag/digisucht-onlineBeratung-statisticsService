package de.caritas.cob.statisticsservice.config;

import static de.caritas.cob.statisticsservice.api.authorization.Authority.CONSULTANT;
import static de.caritas.cob.statisticsservice.api.authorization.Authority.SINGLE_TENANT_ADMIN;
import static de.caritas.cob.statisticsservice.api.authorization.Authority.TENANT_ADMIN;

import de.caritas.cob.statisticsservice.filter.HttpTenantFilter;
import de.caritas.cob.statisticsservice.filter.StatelessCsrfFilter;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Provides the Keycloak/Spring Security configuration.
 */
@Configuration
@KeycloakConfiguration
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

  public static final String[] WHITE_LIST =
      new String[]{"/statistics/docs", "/statistics/docs/**", "/v2/api-docs", "/configuration/ui",
          "/swagger-resources/**", "/configuration/security", "/swagger-ui", "/swagger-ui/**", "/webjars/**", "/healthcheck/health", "/healthcheck/health/**"};

  @Autowired
  AuthorisationService authorisationService;

  @Autowired
  JwtAuthConverterProperties jwtAuthConverterProperties;

  @Value("${csrf.cookie.property}")
  private String csrfCookieProperty;

  @Value("${csrf.header.property}")
  private String csrfHeaderProperty;

  @Autowired
  private Environment environment;

  @Autowired(required = false)
  @Nullable
  private HttpTenantFilter httpTenantFilter;

  @Value("${multitenancy.enabled}")
  private boolean multitenancyEnabled;


  /**
   * Configure spring security filter chain: disable default Spring Boot CSRF token behavior and add
   * custom {@link StatelessCsrfFilter}, set all sessions to be fully stateless, define necessary
   * Keycloak roles for specific REST API paths
   */
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    HttpSecurity httpSecurity = http.csrf(csrf -> csrf.disable())
        .addFilterBefore(new StatelessCsrfFilter(csrfCookieProperty, csrfHeaderProperty),
            CsrfFilter.class);

    if (multitenancyEnabled) {
      httpSecurity = httpSecurity
          .addFilterAfter(httpTenantFilter, BearerTokenAuthenticationFilter.class);
    }

    httpSecurity
        .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests -> requests
        .requestMatchers(WHITE_LIST).permitAll()
        .requestMatchers("/statistics/consultant").hasAuthority(CONSULTANT.getAuthority())
        .requestMatchers("/statistics/registration").hasAnyAuthority(SINGLE_TENANT_ADMIN.getAuthority(),
        TENANT_ADMIN.getAuthority())
        .anyRequest().denyAll());
    httpSecurity.oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
    return httpSecurity.build();
  }

  @Bean
  JwtAuthConverter jwtAuthConverter() {
    return new JwtAuthConverter(jwtAuthConverterProperties, authorisationService);
  }
}
