package de.caritas.cob.statisticsservice.api.tenant;

import static de.caritas.cob.statisticsservice.api.authorization.UserRole.TENANT_ADMIN;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.stereotype.Component;

@Component
public class AllTenantAccessTenantResolver implements TenantResolver {

  private static final long TECHNICAL_TENANT = 0L;

  @Override
  public Optional<Long> resolve(HttpServletRequest request) {
    return isSuperAdminUserRole(request) ? Optional.of(TECHNICAL_TENANT) : Optional.empty();
  }

  private boolean isSuperAdminUserRole(HttpServletRequest request) {
    return containsAnyRole(request, TENANT_ADMIN.getValue());
  }

  private boolean containsAnyRole(HttpServletRequest request, String... expectedRoles) {
    AccessToken token =
        ((KeycloakAuthenticationToken) request.getUserPrincipal())
            .getAccount()
            .getKeycloakSecurityContext()
            .getToken();
    if (hasRoles(token)) {
      Set<String> roles = token.getRealmAccess().getRoles();
      return containsAny(roles, expectedRoles);
    } else {
      return false;
    }
  }

  private boolean containsAny(Set<String> roles, String... expectedRoles) {
    return Arrays.stream(expectedRoles).anyMatch(roles::contains);
  }

  private boolean hasRoles(AccessToken accessToken) {
    return accessToken.getRealmAccess() != null && accessToken.getRealmAccess().getRoles() != null;
  }

  @Override
  public boolean canResolve(HttpServletRequest request) {
    return resolve(request).isPresent();
  }
}
