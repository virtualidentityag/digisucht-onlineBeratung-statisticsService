package de.caritas.cob.statisticsservice.api.tenant;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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

  @InjectMocks
  TenantResolverService tenantResolverService;

  @Mock
  private SecurityContext mockSecurityContext;

  @Mock
  private Authentication mockAuthentication;

  @Mock
  private CustomHeaderTenantResolver customHeaderTenantResolver;

  @Mock
  private AllTenantAccessTenantResolver allTenantAccessTenantResolver;

  @AfterEach
  public void tearDown() {
    ReflectionTestUtils.setField(tenantResolverService, "multitenancyWithSingleDomain", false);
    SecurityContextHolder.clearContext();
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
    SecurityContextHolder.setContext(mockSecurityContext);
    when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
    when(mockAuthentication.isAuthenticated()).thenReturn(true);
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
    assertThrows(AccessDeniedException.class,
        () -> tenantResolverService.resolve(authenticatedRequest));
  }

  @Test
  void resolve_Should_ThrowAccessDeniedExceptionForAuthenticatedUser_IfAccessTokenResolverCannotResolveTenant() {
    // given
    givenUserIsAuthenticated();
    when(accessTokenTenantResolver.canResolve(authenticatedRequest)).thenReturn(false);

    // when, then
    assertThrows(AccessDeniedException.class,
        () -> tenantResolverService.resolve(authenticatedRequest));
  }

  @Test
  void resolve_Should_ThrowAccessDeniedExceptionForNotAuthenticatedUser_IfSubdomainCouldNotBeDetermined() {
    // given
    when(subdomainTenantResolver.canResolve(nonAuthenticatedRequest)).thenReturn(false);
    // when, then
    assertThrows(AccessDeniedException.class,
        () -> tenantResolverService.resolve(nonAuthenticatedRequest));
  }

  @Test
  void resolve_Should_ResolveTenantId_IfSubdomainCouldBeDetermined() {
    // given
    when(subdomainTenantResolver.canResolve(nonAuthenticatedRequest)).thenReturn(true);
    when(subdomainTenantResolver.resolve(nonAuthenticatedRequest)).thenReturn(Optional.of(1L));
    // when
    Long resolved = tenantResolverService.resolve(nonAuthenticatedRequest);
    // then
    assertThat(resolved).isEqualTo(1L);
  }

  @Test
  void resolve_Should_ResolveTenantId_ForTechnicalUserRole() {
    // given
    givenUserIsAuthenticated();
    when(allTenantAccessTenantResolver.canResolve(authenticatedRequest)).thenReturn(true);
    when(allTenantAccessTenantResolver.resolve(authenticatedRequest)).thenReturn(
        Optional.of(TECHNICAL_CONTEXT));

    Long resolved = tenantResolverService.resolve(authenticatedRequest);
    // then
    assertThat(resolved).isEqualTo(TECHNICAL_CONTEXT);
  }
}