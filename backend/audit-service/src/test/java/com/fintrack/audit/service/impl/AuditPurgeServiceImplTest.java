package com.fintrack.audit.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.entity.AuditLog;
import com.mongodb.client.result.DeleteResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class AuditPurgeServiceImplTest {

  @Mock
  private MongoTemplate mongoTemplate;

  @InjectMocks
  private AuditPurgeServiceImpl service;

  @Test
  void realPurgeEmitsAuditPurgeLogForActor() {
    when(mongoTemplate.count(any(Query.class), eq(AuditLog.class))).thenReturn(
      5L
    );
    DeleteResult deleteResult = mock(DeleteResult.class);
    when(deleteResult.getDeletedCount()).thenReturn(3L);
    when(mongoTemplate.remove(any(Query.class), eq(AuditLog.class))).thenReturn(
      deleteResult
    );

    service.purge(30, 0, false, "superadmin");

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(mongoTemplate).save(captor.capture());
    AuditLog logged = captor.getValue();
    assertThat(logged.getAction()).isEqualTo(AuditAction.AUDIT_PURGE);
    assertThat(logged.getUsername()).isEqualTo("superadmin");
    assertThat(logged.getResourceType()).isEqualTo("AUDIT_LOG");
    assertThat(logged.getDetails()).containsEntry("deletedCount", 3L);
  }

  @Test
  void dryRunDoesNotDeleteNorEmitAuditLog() {
    when(mongoTemplate.count(any(Query.class), eq(AuditLog.class))).thenReturn(
      5L
    );

    service.purge(30, 0, true, "superadmin");

    verify(mongoTemplate, never()).remove(any(Query.class), eq(AuditLog.class));
    verify(mongoTemplate, never()).save(any());
  }
}
