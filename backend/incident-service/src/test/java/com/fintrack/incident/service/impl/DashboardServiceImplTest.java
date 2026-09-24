package com.fintrack.incident.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.PeriodType;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.entity.IncidentTypeDistribution;
import com.fintrack.incident.model.readmodel.DashboardMetrics;
import com.fintrack.incident.repository.IncidentHistoryRepository;
import com.fintrack.incident.repository.IncidentRepository;
import com.fintrack.incident.repository.IncidentResolutionCycleRepository;
import com.fintrack.incident.repository.IncidentTypeConfigRepository;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.DashboardService.ComparisonEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private IncidentResolutionCycleRepository cycleRepository;

  @Mock
  private UserClientService userClientService;

  @Mock
  private IncidentTypeConfigRepository incidentTypeConfigRepository;

  @Mock
  private IncidentHistoryRepository incidentHistoryRepository;

  @InjectMocks
  private DashboardServiceImpl dashboardService;

  private UUID userId;
  private UUID serviceId;
  private UUID agencyId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    serviceId = UUID.randomUUID();
    agencyId = UUID.randomUUID();
  }

  @Test
  @DisplayName(
    "Service dashboard metrics use the same service scope as incident stats"
  )
  void serviceMetrics_useCreatorOrTransferredServiceScope() {
    UUID typeId = UUID.randomUUID();
    Incident resolvedIncident = resolvedIncident(typeId);
    UserDetailsImpl user = serviceUser();

    // Count mock stubs
    when(
      incidentRepository.countByStatusInService(
        any(),
        eq(serviceId),
        any(),
        any()
      )
    ).thenReturn(11L);
    org.mockito.Mockito.lenient()
      .when(incidentRepository.countByStatusForService(any(), eq(serviceId)))
      .thenReturn(0L);
    org.mockito.Mockito.lenient()
      .when(
        incidentRepository.countClosedInPeriod(
          any(),
          any(),
          eq(serviceId),
          any(),
          any()
        )
      )
      .thenReturn(2L);
    org.mockito.Mockito.lenient()
      .when(
        incidentRepository.countResolvedInPeriod(
          any(),
          any(),
          eq(serviceId),
          any(),
          any()
        )
      )
      .thenReturn(1L);
    org.mockito.Mockito.lenient()
      .when(
        incidentRepository.countTreatedInPeriod(
          any(),
          any(),
          eq(serviceId),
          any(),
          any()
        )
      )
      .thenReturn(3L);
    org.mockito.Mockito.lenient()
      .when(
        incidentRepository.countRejectedInPeriod(
          any(),
          any(),
          eq(serviceId),
          any(),
          any()
        )
      )
      .thenReturn(1L);
    org.mockito.Mockito.lenient()
      .when(
        incidentRepository.countByStatusForService(
          IncidentStatus.BLOCKED,
          serviceId
        )
      )
      .thenReturn(3L);
    org.mockito.Mockito.lenient()
      .when(
        incidentRepository.countByStatusForService(
          IncidentStatus.OPEN,
          serviceId
        )
      )
      .thenReturn(2L);
    org.mockito.Mockito.lenient()
      .when(
        incidentRepository.countByStatusForService(
          IncidentStatus.PENDING_VALIDATION,
          serviceId
        )
      )
      .thenReturn(3L);
    when(
      incidentRepository.countByCriticalityForService(
        eq(serviceId),
        any(),
        any()
      )
    ).thenReturn(List.of());
    when(incidentRepository.countForService(serviceId)).thenReturn(14L);

    // KPI Lot 2 mock stubs (réouverture = historique RESOLVED->REOPENED, plus reopenCount)
    when(
      cycleRepository.countTransferredScoped(
        isNull(),
        isNull(),
        eq(serviceId),
        isNull(),
        isNull()
      )
    ).thenReturn(1L);
    when(
      incidentRepository.countSlaCompliantInService(eq(serviceId), any(), any())
    ).thenReturn(2L);

    // Lot 3 / Age distribution mock stub
    when(
      incidentRepository.findActiveCreatedAtInService(any(), eq(serviceId))
    ).thenReturn(List.of(LocalDateTime.now()));

    // Recent history / Top resolvers mock stubs
    when(
      incidentHistoryRepository.findRecentForService(
        eq(serviceId),
        any(PageRequest.class)
      )
    ).thenReturn(List.of());
    when(
      incidentRepository.findTopResolversByService(eq(serviceId), any(), any())
    ).thenReturn(List.of());

    // Agregats de delai (une ligne : moyenne, p50, p90, effectif, net, p50 net)
    when(
      cycleRepository.findResolutionStatsScoped(
        null,
        null,
        serviceId,
        null,
        null
      )
    ).thenReturn(List.<Object[]>of(new Object[] { 0.0, 0.0, 0.0, 0L, 0.0, 0.0 }));
    when(
      cycleRepository.findClosureStatsScoped(
        null,
        null,
        serviceId,
        null,
        null
      )
    ).thenReturn(List.<Object[]>of(new Object[] { 2.0, 2.0, 2.0, 1L, 2.0, 2.0 }));
    when(
      incidentRepository.findTypeDistributionForService(serviceId)
    ).thenReturn(List.of(new IncidentTypeDistribution(typeId, "", 6L)));
    // La projection ne porte pas le libelle : il est resolu en une seule requete
    // groupee, pas une par ligne.
    when(incidentTypeConfigRepository.findAllById(anyIterable())).thenReturn(
      List.of(typeConfig(typeId, "Panne informatique"))
    );
    when(
      incidentRepository.countByCriticalityForService(serviceId, null, null)
    ).thenReturn(List.<Object[]>of(new Object[] { Criticality.HIGH, 7L }));
    when(
      incidentRepository.countByAssignedToAndStatusNotIn(eq(userId), any())
    ).thenReturn(0L);

    // Flux : les sorties sont ventilees par statut terminal et les reouvertures
    // depuis un statut terminal reviennent dans le stock.
    when(
      incidentRepository.findTerminalExitsByStatusInPeriod(
        any(),
        isNull(),
        isNull(),
        eq(serviceId),
        isNull(),
        isNull()
      )
    ).thenReturn(
      List.<Object[]>of(
        new Object[] { IncidentStatus.CLOSED.getName(), 5L },
        new Object[] { IncidentStatus.REJECTED.getName(), 2L },
        new Object[] { IncidentStatus.CANCELLED.getName(), 1L }
      )
    );
    when(
      incidentRepository.countBacklogReEntriesInPeriod(
        any(),
        isNull(),
        isNull(),
        eq(serviceId),
        isNull(),
        isNull()
      )
    ).thenReturn(3L);

    DashboardMetrics response = dashboardService.getMetrics(
      "service",
      user,
      null,
      null,
      null,
      2026,
      PeriodType.ALL,
      null,
      null
    );

    assertThat(response.getActiveIncidents()).isEqualTo(11L);
    assertThat(response.getClosedIncidents()).isEqualTo(2L);
    assertThat(response.getTreatedIncidents()).isEqualTo(3L);
    assertThat(response.getRejectedIncidents()).isEqualTo(1L);
    assertThat(response.getBlockedIncidents()).isEqualTo(3L);
    assertThat(response.getTotalIncidents()).isEqualTo(14L);
    assertThat(response.getAvgClosureHours()).isEqualTo(2.0);
    assertThat(response.getDistributionByType()).containsEntry(
      "Panne informatique",
      6L
    );
    assertThat(response.getDistributionByCriticality()).containsEntry(
      Criticality.HIGH.getName(),
      7L
    );

    // Le solde net boucle sur ce qui est affiche : la ventilation somme exactement
    // la sortie, et une reouverture depuis un statut terminal recharge le stock.
    assertThat(response.getExitsClosed()).isEqualTo(5L);
    assertThat(response.getExitsRejected()).isEqualTo(2L);
    assertThat(response.getExitsCancelled()).isEqualTo(1L);
    assertThat(response.getOutflow()).isEqualTo(8L);
    assertThat(response.getBacklogReEntries()).isEqualTo(3L);
    assertThat(response.getInflow()).isEqualTo(14L);
    assertThat(response.getNetBacklog()).isEqualTo(9L);

    // Nouvelles métriques : délai sur fenêtre de clôture (médiane/p90) + aging 4 tranches.
    assertThat(response.getMedianClosureHours()).isEqualTo(2.0);
    assertThat(response.getP90ClosureHours()).isEqualTo(2.0);
    // L'effectif accompagne desormais la mesure.
    assertThat(response.getClosureSampleSize()).isEqualTo(1L);
    // Aucun incident resolu sur la fenetre : la famille resolution reste a zero.
    assertThat(response.getAvgResolutionHours()).isEqualTo(0.0);
    assertThat(response.getAgeDistribution()).containsKeys(
      "0-3",
      "4-7",
      "8-30",
      ">30"
    );
    assertThat(response.getAgeDistribution().get("0-3")).isEqualTo(1L);

    verify(cycleRepository).findClosureStatsScoped(
      null,
      null,
      serviceId,
      null,
      null
    );
    verify(incidentRepository).findTypeDistributionForService(serviceId);
    verify(incidentRepository).countByCriticalityForService(
      serviceId,
      null,
      null
    );
    verify(incidentRepository, never()).countByStatusAndTransferredToService(
      eq(IncidentStatus.OPEN),
      eq(serviceId)
    );
    verify(incidentRepository, never()).countByTransferredToService(serviceId);
    verify(incidentRepository, never()).findByTransferredToService(serviceId);
    verify(incidentRepository, never()).findForService(serviceId);
    verify(incidentRepository, never()).findTypeDistributionByServiceId(
      serviceId
    );
    verify(incidentRepository, never()).countByCriticalityAndServiceId(
      serviceId
    );

    // Comparaison : réutilise le scope service (rappelle getMetrics), une entrée portant les mêmes métriques.
    // Placé après les verify() à appel unique car il déclenche un second calcul.
    List<ComparisonEntry> comparison = dashboardService.getComparison(
      "SERVICE",
      List.of(serviceId),
      user,
      2026,
      PeriodType.ALL,
      null,
      null
    );
    assertThat(comparison).hasSize(1);
    assertThat(comparison.get(0).getId()).isEqualTo(serviceId);
    assertThat(comparison.get(0).getMetrics().getTotalIncidents()).isEqualTo(
      14L
    );
  }

  private UserDetailsImpl serviceUser() {
    return new UserDetailsImpl(
      userId,
      "chef-service",
      null,
      true,
      Set.of(new SimpleGrantedAuthority("INCIDENT_VIEW_SERVICE")),
      serviceId,
      agencyId
    );
  }

  private Incident resolvedIncident(UUID typeId) {
    Incident incident = new Incident();
    incident.setTypeId(typeId);
    incident.setCriticality(Criticality.HIGH);
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedAt(LocalDateTime.of(2026, 5, 19, 8, 0));
    incident.setResolvedAt(LocalDateTime.of(2026, 5, 19, 10, 0));
    incident.setDueDate(LocalDateTime.of(2026, 5, 19, 23, 59, 59));
    return incident;
  }

  private IncidentTypeConfig typeConfig(UUID typeId, String displayName) {
    IncidentTypeConfig config = new IncidentTypeConfig();
    config.setId(typeId);
    config.setName("PANNE_INFORMATIQUE");
    config.setDisplayName(displayName);
    return config;
  }
}
