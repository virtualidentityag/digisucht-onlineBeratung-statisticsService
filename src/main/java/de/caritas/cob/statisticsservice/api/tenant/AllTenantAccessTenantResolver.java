package de.caritas.cob.statisticsservice.api.tenant;

import static de.caritas.cob.statisticsservice.api.authorization.UserRole.TENANT_ADMIN;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AllTenantAccessTenantResolver implements TenantResolver {

  private static final long TECHNICAL_TENANT = 0L;

  @Override
  public Optional<Long> resolve(HttpServletRequest request) {
    return isSuperAdminUserRole() ? Optional.of(TECHNICAL_TENANT) : Optional.empty();
  }

  private boolean isSuperAdminUserRole() {
    return containsAnyRole(TENANT_ADMIN.getValue());
  }

  private boolean containsAnyRole(String... expectedRoles) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      Jwt jwt = (Jwt) authentication.getPrincipal();
      var roles = getRealmRoles(jwt);
      if (roles != null) {
        return containsAny(roles, expectedRoles);
      }
    }
    return false;
  }

  private Collection<String> getRealmRoles(Jwt jwt) {
    if (jwt != null) {
      var claims = jwt.getClaims();
      if (claims.containsKey("realm_access")) {
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
        if (realmAccess.containsKey("roles")) {
          return (List<String>) realmAccess.get("roles");
        }
      }
    }
    return Lists.newArrayList();
  }

  private boolean containsAny(Collection<String> roles, String... expectedRoles) {
    return Arrays.stream(expectedRoles).anyMatch(roles::contains);
  }

  @Override
  public boolean canResolve(HttpServletRequest request) {
    return resolve(request).isPresent();
  }
}
