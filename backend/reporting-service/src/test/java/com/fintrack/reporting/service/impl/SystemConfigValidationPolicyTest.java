package com.fintrack.reporting.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.model.entity.SystemSetting;
import com.fintrack.reporting.model.readmodel.superadmin.SystemThresholds;
import com.fintrack.reporting.model.mapper.superadmin.SuperAdminMapper;
import com.fintrack.reporting.repository.SystemSettingRepository;
import com.fintrack.reporting.security.UserDetailsImpl;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class SystemConfigValidationPolicyTest {
  @Mock private SystemSettingRepository repository;
  @Mock private AuditServiceClientService audit;
  @Mock private MessageSource messages;
  @Mock private UserDetailsImpl actor;
  private SystemConfigServiceImpl service;
  private final Map<String, SystemSetting> store = new HashMap<>();

  @BeforeEach
  void setUp() {
    service = new SystemConfigServiceImpl(repository, audit, messages, new JsonMapper());
    lenient().when(repository.findBySettingKey(anyString()))
      .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
    lenient().when(repository.save(any(SystemSetting.class))).thenAnswer(inv -> {
      SystemSetting value = inv.getArgument(0);
      store.put(value.getSettingKey(), value);
      return value;
    });
    lenient().when(actor.getAuthorities()).thenAnswer(inv ->
      List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
    lenient().when(actor.getId()).thenReturn(UUID.randomUUID());
  }

  @Test
  void defaultsToAdminAndPersistsBothChoicesThroughTheApiMapping() {
    SuperAdminMapper mapper = new SuperAdminMapper(null, null, null);
    assertThat(mapper.toThresholdsResponse(service.getThresholds()).getServiceManagerSelfValidationEnabled()).isZero();
    for (long choice : new long[] {1, 0}) {
      SystemThresholds requested = SystemThresholds.builder().serviceManagerSelfValidationEnabled(choice).build();
      SystemThresholds mapped = mapper.toThresholdsReadModel(mapper.toThresholdsResponse(requested));
      assertThat(service.updateThresholds(mapped, actor).getServiceManagerSelfValidationEnabled()).isEqualTo(choice);
      assertThat(store.get("serviceManagerSelfValidationEnabled").getSettingValue()).isEqualTo(Long.toString(choice));
    }
  }

  @Test
  void refusesInvalidValuesAndNonSuperAdminUpdates() {
    assertThatThrownBy(() -> service.updateThresholds(
      SystemThresholds.builder().serviceManagerSelfValidationEnabled(2L).build(), actor))
      .isInstanceOf(IllegalArgumentException.class);
    when(actor.getAuthorities()).thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    assertThatThrownBy(() -> service.updateThresholds(
      SystemThresholds.builder().serviceManagerSelfValidationEnabled(1L).build(), actor))
      .isInstanceOf(AccessDeniedException.class);
    verify(repository, never()).save(any());
  }
}
