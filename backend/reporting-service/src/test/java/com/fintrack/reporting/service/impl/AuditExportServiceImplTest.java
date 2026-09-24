package com.fintrack.reporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.client.superadmin.SuperAdminAuditClientService;
import com.fintrack.reporting.client.superadmin.SuperAdminUserClient;
import com.fintrack.reporting.client.superadmin.dto.SuperAdminUserClientResponse;
import com.fintrack.reporting.model.readmodel.AuditExportRow;
import com.fintrack.reporting.security.UserDetailsImpl;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuditExportServiceImplTest {

  @Mock
  private SuperAdminAuditClientService auditClientService;

  @Mock
  private SuperAdminUserClient userClient;

  @Mock
  private AuditServiceClientService auditService;

  @Test
  void exportCsv_enrichesKnownLoginRowsAndLocalizesLabels() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage(
      "audit.export.header.timestamp",
      Locale.FRENCH,
      "Horodatage"
    );
    messages.addMessage(
      "audit.export.header.username",
      Locale.FRENCH,
      "Utilisateur"
    );
    messages.addMessage(
      "audit.export.header.userId",
      Locale.FRENCH,
      "ID utilisateur"
    );
    messages.addMessage("audit.export.header.action", Locale.FRENCH, "Action");
    messages.addMessage("audit.export.header.status", Locale.FRENCH, "Statut");
    messages.addMessage(
      "audit.export.header.resourceType",
      Locale.FRENCH,
      "Type de ressource"
    );
    messages.addMessage(
      "audit.export.header.resourceId",
      Locale.FRENCH,
      "ID ressource"
    );
    messages.addMessage(
      "audit.export.header.ipAddress",
      Locale.FRENCH,
      "Adresse IP"
    );
    messages.addMessage(
      "enum.audit_action.LOGIN_FAILURE.name",
      Locale.FRENCH,
      "Échec de connexion"
    );
    messages.addMessage(
      "enum.audit_status.FAILURE.name",
      Locale.FRENCH,
      "Échec"
    );

    AuditExportServiceImpl service = new AuditExportServiceImpl(
      auditClientService,
      userClient,
      auditService,
      messages
    );
    ReflectionTestUtils.setField(service, "auditExportPageSize", 500);
    UUID userId = UUID.randomUUID();
    when(
      auditClientService.getAuditExportRows(0, 500, null, null, null, null)
    ).thenReturn(
      List.of(
        AuditExportRow.builder()
          .timestamp("2026-06-23T14:59:27.324")
          .username("agent3")
          .action("LOGIN_FAILURE")
          .status("FAILURE")
          .resourceType("USER")
          .ipAddress("172.18.0.11")
          .build()
      )
    );
    when(userClient.getUsers()).thenReturn(
      List.of(
        SuperAdminUserClientResponse.builder()
          .id(userId)
          .username("agent3")
          .build()
      )
    );

    LocaleContextHolder.setLocale(Locale.FRENCH);
    byte[] bytes = service.exportCsv(
      null,
      null,
      null,
      null,
      null,
      superAdmin()
    );
    LocaleContextHolder.resetLocaleContext();
    String csv = new String(bytes, StandardCharsets.UTF_8);

    assertThat(csv).startsWith(
      "\uFEFFHorodatage;Utilisateur;ID utilisateur;Action;Statut;Type de ressource;ID ressource;Adresse IP"
    );
    assertThat(csv).contains(
      "agent3;" +
        userId +
        ";Échec de connexion;Échec;USER;" +
        userId +
        ";172.18.0.11"
    );
    verify(userClient).getUsers();
  }

  private UserDetailsImpl superAdmin() {
    return new UserDetailsImpl(
      UUID.randomUUID(),
      "root",
      null,
      true,
      List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
    );
  }
}
