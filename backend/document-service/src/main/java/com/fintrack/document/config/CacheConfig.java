// Configuration Spring : declare les regles techniques liees a cache.

package com.fintrack.document.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

// Configuration activant le mecanisme de cache de Spring pour le service.
@Configuration
@EnableCaching
public class CacheConfig {}
