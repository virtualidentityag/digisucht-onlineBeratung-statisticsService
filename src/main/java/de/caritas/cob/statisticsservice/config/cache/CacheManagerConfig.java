package de.caritas.cob.statisticsservice.config.cache;

import net.sf.ehcache.config.CacheConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheManagerConfig {

  public static final String SESSION_CACHE = "sessionCache";
  public static final String TENANT_CACHE = "tenantCache";

  @Value("${cache.configuration.maxEntriesLocalHeap}")
  private long maxEntriesLocalHeap;

  @Value("${cache.configuration.eternal}")
  private boolean eternal;

  @Value("${cache.configuration.timeToIdleSeconds}")
  private long timeToIdleSeconds;

  @Value("${cache.configuration.timeToLiveSeconds}")
  private long timeToLiveSeconds;

  @Bean
  public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager(SESSION_CACHE, TENANT_CACHE);
  }

  @Bean(destroyMethod = "shutdown")
  public net.sf.ehcache.CacheManager ehCacheManager() {
    var config = new net.sf.ehcache.config.Configuration();
    config.addCache(buildSessionCacheConfiguration());
    config.addCache(buildTenantCacheConfiguration());
    return net.sf.ehcache.CacheManager.newInstance(config);
  }

  private CacheConfiguration buildSessionCacheConfiguration() {
    var agencyCacheConfiguration = new CacheConfiguration();
    agencyCacheConfiguration.setName(SESSION_CACHE);
    agencyCacheConfiguration.setMaxEntriesLocalHeap(maxEntriesLocalHeap);
    agencyCacheConfiguration.setEternal(eternal);
    agencyCacheConfiguration.setTimeToIdleSeconds(timeToIdleSeconds);
    agencyCacheConfiguration.setTimeToLiveSeconds(timeToLiveSeconds);
    return agencyCacheConfiguration;
  }

  private CacheConfiguration buildTenantCacheConfiguration() {
    var tenantCacheConfiguration = new CacheConfiguration();
    tenantCacheConfiguration.setName(TENANT_CACHE);
    tenantCacheConfiguration.setMaxEntriesLocalHeap(maxEntriesLocalHeap);
    tenantCacheConfiguration.setEternal(eternal);
    tenantCacheConfiguration.setTimeToIdleSeconds(timeToIdleSeconds);
    tenantCacheConfiguration.setTimeToLiveSeconds(timeToLiveSeconds);
    return tenantCacheConfiguration;
  }
}
