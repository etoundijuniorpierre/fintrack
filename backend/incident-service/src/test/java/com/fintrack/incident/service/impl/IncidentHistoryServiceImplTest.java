package com.fintrack.incident.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentHistory;
import com.fintrack.incident.repository.IncidentHistoryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentHistoryServiceImplTest {

  @Mock
  private IncidentHistoryRepository historyRepository;

  @InjectMocks
  private IncidentHistoryServiceImpl historyService;

  private UUID incidentId;
  private UUID userId;
  private Incident incident;

  @BeforeEach
  void setUp() {
    incidentId = UUID.randomUUID();
    userId = UUID.randomUUID();

    incident = new Incident();
    incident.setId(incidentId);
    incident.setTitle("Test");
    incident.setDescription("Desc");
    incident.setTypeId(UUID.randomUUID());
    incident.setCriticality(Criticality.MEDIUM);
    incident.setStatus(IncidentStatus.OPEN);
    incident.setCreatedBy(userId);
    incident.setAgencyId(UUID.randomUUID());
  }

  @Test
  @DisplayName("findByIncidentId - Returns history ordered by createdAt")
  void findByIncidentId_ReturnsHistory() {
    IncidentHistory h1 = new IncidentHistory();
    IncidentHistory h2 = new IncidentHistory();
    when(
      historyRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId)
    ).thenReturn(List.of(h1, h2));

    List<IncidentHistory> result = historyService.findByIncidentId(incidentId);

    assertThat(result).hasSize(2);
    verify(historyRepository).findByIncidentIdOrderByCreatedAtAsc(incidentId);
  }

  @Test
  @DisplayName("record - Saves history entry with all fields")
  void record_ValidData_SavesHistory() {
    IncidentHistory saved = new IncidentHistory();
    saved.setId(UUID.randomUUID());
    when(historyRepository.save(any())).thenReturn(saved);

    IncidentHistory result = historyService.record(
      incident,
      userId,
      ActionType.CREATION,
      null,
      "OPEN",
      null
    );

    assertThat(result).isNotNull();
    verify(historyRepository).save(any(IncidentHistory.class));
  }

  @Test
  @DisplayName("record - Sets all fields on history entity")
  void record_SetsAllFields() {
    when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    IncidentHistory result = historyService.record(
      incident,
      userId,
      ActionType.STATUS_CHANGE,
      "OPEN",
      "VALIDATED",
      "comment"
    );

    assertThat(result.getIncident()).isEqualTo(incident);
    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getAction()).isEqualTo(ActionType.STATUS_CHANGE);
    assertThat(result.getOldValue()).isEqualTo("OPEN");
    assertThat(result.getNewValue()).isEqualTo("VALIDATED");
    assertThat(result.getComment()).isEqualTo("comment");
  }

  @Test
  @DisplayName("record - Handles null oldValue and comment")
  void record_NullOptionalFields_SavesSuccessfully() {
    when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    IncidentHistory result = historyService.record(
      incident,
      userId,
      ActionType.CREATION,
      null,
      "OPEN",
      null
    );

    assertThat(result.getOldValue()).isNull();
    assertThat(result.getComment()).isNull();
  }
}
