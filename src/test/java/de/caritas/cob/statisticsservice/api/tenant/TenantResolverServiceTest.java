package de.caritas.cob.statisticsservice.api.tenant;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TenantResolverServiceTest {

  public static final long TECHNICAL_CONTEXT = 0L;

  @Mock
  SubdomainTenantResolver subdomainTenantResolver;

  @Mock
  AccessTokenTenantResolver accessTokenTenantResolver;

  @Mock
  HttpServletRequest authenticatedRequest;

  @Mock
  HttpServletRequest nonAuthenticatedRequest;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  KeycloakAuthenticationToken token;

  @InjectMocks
  TenantResolverService tenantResolverService;

  @Mock
  private CustomHeaderTenantResolver customHeaderTenantResolver;

  @Mock
  private AllTenantAccessTenantResolver allTenantAccessTenantResolver;

  @AfterEach
  public void tearDown() {
    ReflectionTestUtils.setField(tenantResolverService, "multitenancyWithSingleDomain", false);
  }


  @Test
  void resolve_Should_ResolveFromAccessTokenForAuthenticatedUser_And_PassValidation() {
    // given
    givenUserIsAuthenticated();
    when(accessTokenTenantResolver.canResolve(authenticatedRequest)).thenReturn(true);
    when(accessTokenTenantResolver.resolve(authenticatedRequest)).thenReturn(Optional.of(1L));
    when(subdomainTenantResolver.canResolve(authenticatedRequest)).thenReturn(true);
    when(subdomainTenantResolver.resolve(authenticatedRequest)).thenReturn(Optional.of(1L));

    // when
    Long resolvedTenantId = tenantResolverService.resolve(authenticatedRequest);

    // then
    assertThat(resolvedTenantId).isEqualTo(1L);
  }

  private void givenUserIsAuthenticated() {
    when(authenticatedRequest.getUserPrincipal()).thenReturn(token);
  }

  @Test
  void resolve_Should_ThrowAccessDeniedException_ForAuthenticatedUser_When_SubdomainTenantIdDoesNotMatchTenantIdFromToken() {
    // given

    givenUserIsAuthenticated();
    when(accessTokenTenantResolver.canResolve(authenticatedRequest)).thenReturn(true);
    when(accessTokenTenantResolver.resolve(authenticatedRequest)).thenReturn(Optional.of(1L));
    when(subdomainTenantResolver.canResolve(authenticatedRequest)).thenReturn(true);
    when(subdomainTenantResolver.resolve(authenticatedRequest)).thenReturn(Optional.of(2L));

    // when, then
    assertThrows(AccessDeniedException.class, () -> tenantResolverService.resolve(authenticatedRequest));
  }

  @Test
  void resolve_Should_ThrowAccessDeniedExceptionForAuthenticatedUser_IfAccessTokenResolverCannotResolveTenant() {
    // given
    givenUserIsAuthenticated();
    when(accessTokenTenantResolver.canResolve(authenticatedRequest)).thenReturn(false);

    // when, then
    assertThrows(AccessDeniedException.class, () -> tenantResolverService.resolve(authenticatedRequest));
  }

  @Test
  void resolve_Should_NotResolveTenantAndThrowAccessDeniedException_When_NoResolversCouldResolveAndNormalMultitenancyModeForNonAuthUsers() {
    assertThrows(AccessDeniedException.class, () -> tenantResolverService.resolve(nonAuthenticatedRequest));
  }

  @Test
  void resolve_Should_ResolveFromSubdomainForNonAuthUsersAndNormalMultitenancyMode() {
    when(subdomainTenantResolver.canResolve(nonAuthenticatedRequest)).thenReturn(true);
    when(subdomainTenantResolver.resolve(nonAuthenticatedRequest)).thenReturn(Optional.of(2L));
    assertThat(tenantResolverService.resolve(nonAuthenticatedRequest)).isEqualTo(2L);
  }

  @Test
  void resolve_Should_ReturnNullTenantForNonAuthUsersAndSingleDomainMultitenancyMode() {
    // given
    ReflectionTestUtils.setField(tenantResolverService, "multitenancyWithSingleDomain", true);
    // when, then
    assertThat(tenantResolverService.resolve(nonAuthenticatedRequest)).isNull();
  }

  @Test
  void resolve_Should_ResolveTenantId_ForTechnicalUserRole() {
    // given
    givenUserIsAuthenticated();
    when(allTenantAccessTenantResolver.canResolve(authenticatedRequest)).thenReturn(true);
    when(allTenantAccessTenantResolver.resolve(authenticatedRequest)).thenReturn(Optional.of(TECHNICAL_CONTEXT));

    // when
    Long resolved = tenantResolverService.resolve(authenticatedRequest);
    // then
    assertThat(resolved).isEqualTo(TECHNICAL_CONTEXT);
  }
}