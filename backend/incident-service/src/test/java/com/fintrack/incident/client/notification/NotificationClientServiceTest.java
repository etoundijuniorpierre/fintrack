// Tests unitaires : verifie le routage des notifications vers le valideur configure.
package com.fintrack.incident.client.notification;

import com.fintrack.common.notification.EmailNotificationEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.incident.client.notification.dto.NotificationClientRequest;
import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.security.IncidentValidationPolicy;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.IncidentValidatorScope;
import com.fintrack.incident.model.dto.response.AgencySummaryResponse;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.readmodel.ExternalUser;
import com.fintrack.incident.service.IncidentTypeConfigService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class NotificationClientServiceTest {

  @Mock
  private NotificationClient notificationClient;

  @Mock
  private UserClientService userClientService;

  @Mock
  private IncidentTypeConfigService incidentTypeConfigService;

  @Mock
  private MessageSource messageSource;

  @Mock
  private ReportingSystemConfigClientService systemConfig;

  @Mock
  private com.fintrack.incident.client.reporting.ReportingEmailNotificationConfigService emailNotificationConfig;

  private NotificationClientService notificationService;
  private Incident incident;

  @BeforeEach
  void setUp() {
    notificationService = new NotificationClientService(
      notificationClient,
      userClientService,
      incidentTypeConfigService,
      messageSource,
      new IncidentValidationPolicy(systemConfig, userClientService),
      emailNotificationConfig
    );
    // Par defaut, la config globale autorise tous les e-mails, sans exclusion :
    // les tests existants valident donc le comportement "e-mail actif".
    lenient()
      .when(
        emailNotificationConfig.isEmailEnabled(
          any(com.fintrack.common.notification.EmailNotificationEvent.class)
        )
      )
      .thenReturn(true);
    lenient()
      .when(
        emailNotificationConfig.excludedRecipients(
          any(com.fintrack.common.notification.EmailNotificationEvent.class)
        )
      )
      .thenReturn(java.util.Set.of());
    lenient().when(
      messageSource.getMessage(
        anyString(),
        any(Object[].class),
        anyString(),
        any(Locale.class)
      )
    ).thenAnswer(invocation -> invocation.getArgument(0));

    incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setTitle("Payment outage");
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    incident.setAgencyId(UUID.randomUUID());
    AgencySummaryResponse agency = new AgencySummaryResponse();
    agency.setId(incident.getAgencyId());
    agency.setName("Agency Alpha");
    lenient()
      .when(userClientService.resolveAgency(incident.getAgencyId()))
      .thenReturn(agency);
  }

  @Test
  @DisplayName(
    "notifyIncidentSubmitted - Should notify the agency manager for agency scope"
  )
  void notifyIncidentSubmitted_AgencyScope_NotifiesAgencyManager() {
    UUID managerId = UUID.randomUUID();
    when(
      userClientService.resolveAgencyHead(incident.getAgencyId())
    ).thenReturn(managerId);
    when(userClientService.getUser(managerId)).thenReturn(
      user(managerId, "agency.manager")
    );

    notificationService.notifyIncidentSubmitted(
      incident,
      IncidentValidatorScope.AGENCY_MANAGER,
      null,
      null
    );

    NotificationClientRequest request = captureNotification();
    assertThat(request.getRecipient()).isEqualTo("agency.manager");
    assertThat(request.getSubject()).isEqualTo(
      "notification.incident.subject.context"
    );
    assertThat(request.getTemplateParams())
      .containsEntry("agency_id", incident.getAgencyId().toString())
      .containsEntry("agency_name", "Agency Alpha")
      .containsEntry("agency_name_en", "Agency Alpha")
      .containsEntry("incident_agency", "Agency Alpha")
      .containsEntry("incident_status_code", "PENDING_VALIDATION");
    verify(userClientService, never()).resolveServiceHead(any());
  }

  @Test
  @DisplayName(
    "notifyIncidentSubmitted - Should notify the target service manager for service scope"
  )
  void notifyIncidentSubmitted_ServiceScope_NotifiesServiceManager() {
    UUID targetServiceId = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();
    // Le service actif de l'incident differe du service cible du type : c'est le type
    // qui designe le valideur, comme le fait IncidentWorkflowGuard.
    incident.setTransferredToService(UUID.randomUUID());
    configureTargetService(targetServiceId);
    when(userClientService.resolveServiceHead(targetServiceId)).thenReturn(
      managerId
    );
    when(userClientService.getUser(managerId)).thenReturn(
      user(managerId, "service.manager")
    );

    notificationService.notifyIncidentSubmitted(
      incident,
      IncidentValidatorScope.TARGET_SERVICE_MANAGER,
      null,
      null
    );

    NotificationClientRequest request = captureNotification();
    assertThat(request.getRecipient()).isEqualTo("service.manager");
    verify(userClientService, never()).resolveAgencyHead(any());
  }

  @Test
  @DisplayName(
    "notifyIncidentSubmitted - Target-service scope falls back to the agency manager when the type has no target service"
  )
  void notifyIncidentSubmitted_TargetServiceScope_NoTargetService_FallsBackToAgency() {
    UUID agencyManagerId = UUID.randomUUID();
    configureTargetService(null);
    when(
      userClientService.resolveAgencyHead(incident.getAgencyId())
    ).thenReturn(agencyManagerId);
    when(userClientService.getUser(agencyManagerId)).thenReturn(
      user(agencyManagerId, "agency.manager")
    );

    notificationService.notifyIncidentSubmitted(
      incident,
      IncidentValidatorScope.TARGET_SERVICE_MANAGER,
      null,
      null
    );

    NotificationClientRequest request = captureNotification();
    assertThat(request.getRecipient()).isEqualTo("agency.manager");
  }

  @Test
  @DisplayName(
    "notifyIncidentSubmitted - Should notify the creator's service manager for source-service scope"
  )
  void notifyIncidentSubmitted_SourceServiceScope_NotifiesCreatorServiceManager() {
    UUID serviceId = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();
    incident.setCreatorServiceId(serviceId);
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(managerId);
    when(userClientService.getUser(managerId)).thenReturn(
      user(managerId, "source.service.manager")
    );

    notificationService.notifyIncidentSubmitted(
      incident,
      IncidentValidatorScope.SOURCE_SERVICE_MANAGER,
      null,
      null
    );

    NotificationClientRequest request = captureNotification();
    assertThat(request.getRecipient()).isEqualTo("source.service.manager");
    // Le service du createur a un chef : pas de repli agence.
    verify(userClientService, never()).resolveAgencyHead(any());
  }

  @Test
  @DisplayName(
    "notifyIncidentSubmitted - Source-service scope falls back to the agency manager when the creator's service has no head"
  )
  void notifyIncidentSubmitted_SourceServiceScope_NoHead_FallsBackToAgency() {
    UUID serviceId = UUID.randomUUID();
    UUID agencyManagerId = UUID.randomUUID();
    incident.setCreatorServiceId(serviceId);
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(null);
    when(
      userClientService.resolveAgencyHead(incident.getAgencyId())
    ).thenReturn(agencyManagerId);
    when(userClientService.getUser(agencyManagerId)).thenReturn(
      user(agencyManagerId, "agency.manager")
    );

    notificationService.notifyIncidentSubmitted(
      incident,
      IncidentValidatorScope.SOURCE_SERVICE_MANAGER,
      null,
      null
    );

    NotificationClientRequest request = captureNotification();
    assertThat(request.getRecipient()).isEqualTo("agency.manager");
  }

  @Test
  @DisplayName(
    "notifyIncidentSubmitted - Admin scope notifies every administrator (no agency fallback)"
  )
  void notifyIncidentSubmitted_AdminScope_NotifiesAllAdmins() {
    ExternalUser admin1 = user(UUID.randomUUID(), "admin.one");
    ExternalUser admin2 = user(UUID.randomUUID(), "admin.two");
    when(userClientService.resolveAdmins()).thenReturn(
      List.of(admin1, admin2)
    );

    notificationService.notifyIncidentSubmitted(
      incident,
      IncidentValidatorScope.ADMIN,
      null,
      null
    );

    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient, times(2)).createNotification(captor.capture());
    assertThat(
      captor
        .getAllValues()
        .stream()
        .map(NotificationClientRequest::getRecipient)
        .toList()
    ).containsExactlyInAnyOrder("admin.one", "admin.two");
    verify(userClientService, never()).resolveAgencyHead(any());
  }

  @Test
  @DisplayName(
    "notifyIncidentSubmitted - Should send one notification when creator is also validator"
  )
  void notifyIncidentSubmitted_CreatorIsValidator_SendsSingleNotification() {
    UUID managerId = UUID.randomUUID();
    incident.setCreatedBy(managerId);
    when(
      userClientService.resolveAgencyHead(incident.getAgencyId())
    ).thenReturn(managerId);
    when(userClientService.getUser(managerId)).thenReturn(
      user(managerId, "agency.manager")
    );

    notificationService.notifyIncidentSubmitted(
      incident,
      IncidentValidatorScope.AGENCY_MANAGER,
      managerId,
      null
    );

    NotificationClientRequest request = captureNotification();
    assertThat(request.getRecipient()).isEqualTo("agency.manager");
    assertThat(request.getSubject()).isEqualTo(
      "notification.incident.subject.context"
    );
    verify(notificationClient, times(1)).createNotification(any());
  }

  @Test
  @DisplayName(
    "notifyIncidentAssigned - Personalizes the message for the creator and assignee"
  )
  void notifyIncidentAssigned_PersonalizesRecipients() {
    UUID creatorId = UUID.randomUUID();
    UUID assigneeId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.OPEN);
    incident.setCreatedBy(creatorId);
    incident.setAssignedTo(assigneeId);
    when(userClientService.getUser(creatorId)).thenReturn(
      user(creatorId, "creator")
    );
    when(userClientService.getUser(assigneeId)).thenReturn(
      user(assigneeId, "assignee")
    );

    notificationService.notifyIncidentAssigned(incident, assigneeId);

    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient, times(2)).createNotification(captor.capture());
    List<String> contents = captor
      .getAllValues()
      .stream()
      .map(NotificationClientRequest::getContent)
      .toList();
    assertThat(contents).containsExactlyInAnyOrder(
      "notification.incident.assigned.content.creator",
      "notification.incident.assigned.content.assignee"
    );
  }

  @Test
  @DisplayName(
    "Email notifications - Sends creation updates through internal and email channels"
  )
  void emailNotifications_Creation_SendsBothChannels() {
    UUID creatorId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.OPEN);
    incident.setCreatedBy(creatorId);
    enableEmailNotifications();
    when(userClientService.getUser(creatorId)).thenReturn(
      user(creatorId, "creator")
    );

    notificationService.notifyIncidentSubmitted(
      incident,
      IncidentValidatorScope.AGENCY_MANAGER,
      creatorId,
      null
    );

    assertNotificationChannels(1, 1);
  }

  @Test
  @DisplayName(
    "Email notifications - Sends assignment updates through internal and email channels"
  )
  void emailNotifications_Assignment_SendsBothChannels() {
    UUID creatorId = UUID.randomUUID();
    UUID assigneeId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setCreatedBy(creatorId);
    incident.setAssignedTo(assigneeId);
    enableEmailNotifications();
    when(userClientService.getUser(creatorId)).thenReturn(
      user(creatorId, "creator")
    );
    when(userClientService.getUser(assigneeId)).thenReturn(
      user(assigneeId, "assignee")
    );

    notificationService.notifyIncidentAssigned(incident, assigneeId);

    assertNotificationChannels(2, 2);
  }

  @ParameterizedTest
  @EnumSource(value = IncidentStatus.class, names = { "RESOLVED", "CLOSED" })
  @DisplayName(
    "Email notifications - Sends resolution and closure updates through both channels"
  )
  void emailNotifications_FinalStatuses_SendBothChannels(
    IncidentStatus status
  ) {
    UUID creatorId = UUID.randomUUID();
    UUID assigneeId = UUID.randomUUID();
    incident.setStatus(status);
    incident.setCreatedBy(creatorId);
    incident.setAssignedTo(assigneeId);
    enableEmailNotifications();
    when(userClientService.getUser(creatorId)).thenReturn(
      user(creatorId, "creator")
    );
    when(userClientService.getUser(assigneeId)).thenReturn(
      user(assigneeId, "assignee")
    );

    notificationService.notifyStatusChanged(
      incident,
      status.name(),
      assigneeId,
      null
    );

    assertNotificationChannels(2, 2);
  }

  @Test
  @DisplayName(
    "notifyStatusChanged - Covers every actor of the treated/resolved/closed chain"
  )
  void notifyStatusChanged_NotifiesWholeProcessingChain() {
    UUID creatorId = UUID.randomUUID();
    UUID assigneeId = UUID.randomUUID();
    UUID validatorId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    UUID serviceHeadId = UUID.randomUUID();
    UUID agencyHeadId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();

    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedBy(creatorId);
    incident.setAssignedTo(assigneeId);
    incident.setValidatedBy(validatorId);
    incident.setCreatorServiceId(serviceId);

    when(userClientService.getUser(creatorId)).thenReturn(
      user(creatorId, "creator")
    );
    when(userClientService.getUser(assigneeId)).thenReturn(
      user(assigneeId, "assignee")
    );
    when(userClientService.getUser(validatorId)).thenReturn(
      user(validatorId, "validator")
    );
    when(userClientService.getUser(actorId)).thenReturn(
      user(actorId, "actor")
    );
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(
      serviceHeadId
    );
    when(userClientService.getUser(serviceHeadId)).thenReturn(
      user(serviceHeadId, "service.head")
    );
    when(
      userClientService.resolveAgencyHead(incident.getAgencyId())
    ).thenReturn(agencyHeadId);
    when(userClientService.getUser(agencyHeadId)).thenReturn(
      user(agencyHeadId, "agency.head")
    );
    when(userClientService.resolveAdmins()).thenReturn(
      List.of(user(adminId, "admin"))
    );

    notificationService.notifyStatusChanged(
      incident,
      IncidentStatus.RESOLVED.name(),
      actorId,
      null
    );

    // Aucun maillon de la chaine ne doit etre oublie : sans notification, l'utilisateur
    // ne recoit pas non plus le rafraichissement temps reel de ses pages.
    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient, times(7)).createNotification(captor.capture());
    assertThat(captor.getAllValues())
      .extracting(NotificationClientRequest::getRecipient)
      .containsExactlyInAnyOrder(
        "creator",
        "assignee",
        "validator",
        "actor",
        "service.head",
        "agency.head",
        "admin"
      );
  }

  @Test
  @DisplayName(
    "notifyStatusChanged - Keeps the detached handler informed on reopening"
  )
  void notifyStatusChanged_ExtraRecipients_ReachesDetachedHandler() {
    UUID creatorId = UUID.randomUUID();
    UUID previousAssigneeId = UUID.randomUUID();

    incident.setStatus(IncidentStatus.REOPENED);
    incident.setCreatedBy(creatorId);
    // La reouverture detache le traitant avant la notification : il n'est plus
    // porte par l'incident et ne peut arriver que par les destinataires ajoutes.
    incident.setAssignedTo(null);

    when(userClientService.getUser(creatorId)).thenReturn(
      user(creatorId, "creator")
    );
    when(userClientService.getUser(previousAssigneeId)).thenReturn(
      user(previousAssigneeId, "previous.assignee")
    );

    notificationService.notifyStatusChanged(
      incident,
      IncidentStatus.REOPENED.name(),
      creatorId,
      "motif",
      java.util.Set.of(previousAssigneeId)
    );

    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient, times(2)).createNotification(captor.capture());
    assertThat(captor.getAllValues())
      .extracting(NotificationClientRequest::getRecipient)
      .containsExactlyInAnyOrder("creator", "previous.assignee");
  }

  @Test
  @DisplayName(
    "Internal notifications - Remain enabled for legacy disabled configurations"
  )
  void internalNotifications_LegacyDisabledConfiguration_StillSends() {
    UUID creatorId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.CLOSED);
    incident.setCreatedBy(creatorId);
    configureNotifications(false, false);
    when(userClientService.getUser(creatorId)).thenReturn(
      user(creatorId, "creator")
    );

    notificationService.notifyStatusChanged(
      incident,
      IncidentStatus.CLOSED.name(),
      creatorId,
      null
    );

    assertNotificationChannels(1, 0);
  }

  @Test
  @DisplayName("notifyIncidentComment - Includes the author, incident and agency in the subject")
  void notifyIncidentComment_ContextualizesSubject() {
    UUID authorId = UUID.randomUUID();
    incident.setCreatedBy(authorId);
    ExternalUser author = ExternalUser.builder()
      .id(authorId)
      .username("fintrack")
      .firstName("System")
      .lastName("Administrator")
      .email("admin@fintrack.test")
      .build();
    when(userClientService.getUser(authorId)).thenReturn(author);
    when(
      messageSource.getMessage(
        eq("notification.incident.comment.subject"),
        any(Object[].class),
        anyString(),
        eq(Locale.FRENCH)
      )
    ).thenAnswer(invocation ->
      "Commentaire ajouté par " +
      ((Object[]) invocation.getArgument(1))[0]
    );
    when(
      messageSource.getMessage(
        eq("notification.incident.comment.subject"),
        any(Object[].class),
        anyString(),
        eq(Locale.ENGLISH)
      )
    ).thenAnswer(invocation ->
      "Comment added by " +
      ((Object[]) invocation.getArgument(1))[0]
    );
    when(
      messageSource.getMessage(
        eq("notification.incident.subject.context"),
        any(Object[].class),
        anyString(),
        eq(Locale.FRENCH)
      )
    ).thenAnswer(invocation -> {
      Object[] args = invocation.getArgument(1);
      return args[0] + " — « " + args[1] + " » — " + args[2];
    });
    when(
      messageSource.getMessage(
        eq("notification.incident.subject.context"),
        any(Object[].class),
        anyString(),
        eq(Locale.ENGLISH)
      )
    ).thenAnswer(invocation -> {
      Object[] args = invocation.getArgument(1);
      return args[0] + " — \"" + args[1] + "\" — " + args[2];
    });

    notificationService.notifyIncidentComment(incident, authorId);

    NotificationClientRequest request = captureNotification();
    assertThat(request.getSubject()).isEqualTo(
      "Commentaire ajouté par System Administrator — « Payment outage » — Agency Alpha"
    );
    assertThat(request.getSubjectEn()).isEqualTo(
      "Comment added by System Administrator — \"Payment outage\" — Agency Alpha"
    );
  }

  @Test
  @DisplayName(
    "notifyIncidentComment - Notifies the circle of the incident, administrators included"
  )
  void notifyIncidentComment_NotifiesCircleAndAdmins() {
    UUID authorId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID assigneeId = UUID.randomUUID();
    UUID serviceHeadId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();

    incident.setCreatedBy(creatorId);
    incident.setAssignedTo(assigneeId);
    incident.setTransferredToService(serviceId);

    when(userClientService.getUser(authorId)).thenReturn(
      user(authorId, "comment.author")
    );
    when(userClientService.getUser(creatorId)).thenReturn(
      user(creatorId, "incident.creator")
    );
    when(userClientService.getUser(assigneeId)).thenReturn(
      user(assigneeId, "incident.assignee")
    );
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(
      serviceHeadId
    );
    when(userClientService.getUser(serviceHeadId)).thenReturn(
      user(serviceHeadId, "service.head")
    );
    when(userClientService.resolveAdmins()).thenReturn(
      List.of(user(adminId, "the.admin"))
    );

    notificationService.notifyIncidentComment(incident, authorId);

    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient, atLeastOnce()).createNotification(
      captor.capture()
    );
    // L'administrateur suit la vie de l'incident au meme titre que le cercle direct.
    assertThat(
      captor
        .getAllValues()
        .stream()
        .map(NotificationClientRequest::getRecipient)
        .toList()
    ).contains(
      "comment.author",
      "incident.creator",
      "incident.assignee",
      "service.head",
      "the.admin"
    );
  }

  @Test
  @DisplayName(
    "notifyConfirmationRequested - Reaches the source service head, who alone can answer"
  )
  void notifyConfirmationRequested_ReachesTheSourceServiceHead() {
    UUID requesterId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID sourceServiceId = UUID.randomUUID();
    UUID sourceHeadId = UUID.randomUUID();

    incident.setCreatedBy(creatorId);
    incident.setCreatorServiceId(sourceServiceId);

    when(userClientService.getUser(requesterId)).thenReturn(
      user(requesterId, "the.handler")
    );
    when(userClientService.getUser(creatorId)).thenReturn(
      user(creatorId, "incident.creator")
    );
    when(userClientService.resolveServiceHead(sourceServiceId)).thenReturn(
      sourceHeadId
    );
    when(userClientService.getUser(sourceHeadId)).thenReturn(
      user(sourceHeadId, "source.head")
    );

    notificationService.notifyConfirmationRequested(
      incident,
      requesterId,
      "toujours d'actualite ?"
    );

    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient, atLeastOnce()).createNotification(
      captor.capture()
    );
    assertThat(
      captor
        .getAllValues()
        .stream()
        .map(NotificationClientRequest::getRecipient)
        .toList()
    ).contains("source.head", "incident.creator");
  }

  @Test
  @DisplayName(
    "notifyConfirmationRequested - Falls back to the agency head when the source service has none"
  )
  void notifyConfirmationRequested_FallsBackToTheAgencyHead() {
    UUID requesterId = UUID.randomUUID();
    UUID agencyHeadId = UUID.randomUUID();

    incident.setCreatorServiceId(null);
    when(userClientService.resolveAgencyHead(incident.getAgencyId())).thenReturn(
      agencyHeadId
    );
    when(userClientService.getUser(agencyHeadId)).thenReturn(
      user(agencyHeadId, "agency.head")
    );

    notificationService.notifyConfirmationRequested(incident, requesterId, null);

    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient, atLeastOnce()).createNotification(
      captor.capture()
    );
    assertThat(
      captor
        .getAllValues()
        .stream()
        .map(NotificationClientRequest::getRecipient)
        .toList()
    ).contains("agency.head");
  }

  @Test
  @DisplayName(
    "notifyIncidentCommentReply - Notifies the parent author and administrators"
  )
  void notifyIncidentCommentReply_NotifiesParentAuthorAndAdmins() {
    UUID replyAuthorId = UUID.randomUUID();
    UUID parentAuthorId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();

    when(userClientService.getUser(replyAuthorId)).thenReturn(
      user(replyAuthorId, "reply.author")
    );
    when(userClientService.getUser(parentAuthorId)).thenReturn(
      user(parentAuthorId, "parent.author")
    );
    when(userClientService.resolveAdmins()).thenReturn(
      List.of(user(adminId, "the.admin"))
    );

    notificationService.notifyIncidentCommentReply(
      incident,
      replyAuthorId,
      parentAuthorId
    );

    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient, atLeastOnce()).createNotification(
      captor.capture()
    );
    assertThat(
      captor
        .getAllValues()
        .stream()
        .map(NotificationClientRequest::getRecipient)
        .toList()
    ).contains("reply.author", "parent.author", "the.admin");
  }

  @Test
  @DisplayName("notifyIncidentTransferredToAgency - Should notify the agency head")
  void notifyIncidentTransferredToAgency_NotifiesAgencyHead() {
    UUID agencyHeadId = UUID.randomUUID();
    when(
      userClientService.resolveAgencyHead(incident.getAgencyId())
    ).thenReturn(agencyHeadId);
    when(userClientService.getUser(agencyHeadId)).thenReturn(
      user(agencyHeadId, "agency.head")
    );

    notificationService.notifyIncidentTransferredToAgency(
      incident,
      null,
      incident.getAgencyId(),
      null
    );

    NotificationClientRequest request = captureNotification();
    assertThat(request.getRecipient()).isEqualTo("agency.head");
    assertThat(request.getSubject()).isEqualTo(
      "notification.incident.subject.context"
    );
  }

  @Test
  @DisplayName(
    "Email config - Global toggle off suppresses the email but keeps the internal notification"
  )
  void emailConfig_GlobalToggleOff_SuppressesEmailKeepsInternal() {
    UUID creatorId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedBy(creatorId);
    enableEmailNotifications();
    when(
      emailNotificationConfig.isEmailEnabled(
        EmailNotificationEvent.STATUS_CHANGED
      )
    ).thenReturn(false);
    when(userClientService.getUser(creatorId)).thenReturn(
      user(creatorId, "creator")
    );

    notificationService.notifyStatusChanged(
      incident,
      IncidentStatus.RESOLVED.name(),
      creatorId,
      null
    );

    // Interrupteur global coupe : plus d'e-mail, mais l'interne reste.
    assertNotificationChannels(1, 0);
  }

  @Test
  @DisplayName(
    "Email config - Excluded user is filtered from email even though still a recipient"
  )
  void emailConfig_ExcludedUser_FilteredFromEmailOnly() {
    UUID creatorId = UUID.randomUUID();
    UUID assigneeId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedBy(creatorId);
    incident.setAssignedTo(assigneeId);
    enableEmailNotifications();
    when(
      emailNotificationConfig.excludedRecipients(
        EmailNotificationEvent.STATUS_CHANGED
      )
    ).thenReturn(java.util.Set.of(creatorId));
    when(userClientService.getUser(creatorId)).thenReturn(
      user(creatorId, "creator")
    );
    when(userClientService.getUser(assigneeId)).thenReturn(
      user(assigneeId, "assignee")
    );

    notificationService.notifyStatusChanged(
      incident,
      IncidentStatus.RESOLVED.name(),
      creatorId,
      null
    );

    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient, org.mockito.Mockito.atLeastOnce())
      .createNotification(captor.capture());
    // Le createur exclu reste notifie en interne mais jamais par e-mail ; l'assigne
    // (non exclu) recoit bien l'e-mail.
    List<String> emailRecipients = captor
      .getAllValues()
      .stream()
      .filter(r -> "EMAIL".equals(r.getType()))
      .map(NotificationClientRequest::getRecipient)
      .toList();
    assertThat(emailRecipients).contains("assignee@fintrack.test");
    assertThat(emailRecipients).doesNotContain("creator@fintrack.test");
    assertThat(
      captor
        .getAllValues()
        .stream()
        .filter(r -> "INTERNAL".equals(r.getType()))
        .map(NotificationClientRequest::getRecipient)
    ).contains("creator", "assignee");
  }

  @Test
  @DisplayName(
    "Email config - Direction toggle off cuts only the email, internal stays unchanged"
  )
  void emailConfig_DirectionToggleOff_CutsOnlyEmail() {
    UUID assigneeId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    incident.setAssignedTo(assigneeId);
    when(
      emailNotificationConfig.isEmailEnabled(
        EmailNotificationEvent.DIRECTION_REJECTED
      )
    ).thenReturn(false);
    when(userClientService.getUser(assigneeId)).thenReturn(
      user(assigneeId, "assignee")
    );
    when(userClientService.resolveAdmins()).thenReturn(
      List.of(user(adminId, "admin"))
    );

    notificationService.notifyDirectionRejected(incident, null, "motif");

    // Interne : assigne + admin. E-mail : aucun (interrupteur Direction coupe).
    assertNotificationChannels(2, 0);
  }

  @Test
  @DisplayName(
    "Email config - Daily late-incidents report is fully suppressed when its toggle is off"
  )
  void emailConfig_DailyReportToggleOff_SendsNothing() {
    when(
      emailNotificationConfig.isEmailEnabled(
        EmailNotificationEvent.LATE_INCIDENTS_DAILY_REPORT
      )
    ).thenReturn(false);

    notificationService.notifyLateIncidentsDailyReport(List.of(incident));

    verify(notificationClient, never()).createNotification(any());
  }

  // Capture la notification interne produite par le service.
  @Test
  void ownServiceCreatorValidationReminderGoesToAdminByDefault() {
    UUID serviceId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    incident.setCreatedBy(creatorId);
    incident.setTypeId(UUID.randomUUID());
    IncidentTypeConfig config = new IncidentTypeConfig();
    config.setDefaultTargetServiceId(serviceId);
    config.setValidatorScope(IncidentValidatorScope.SOURCE_SERVICE_MANAGER);
    when(incidentTypeConfigService.findById(incident.getTypeId())).thenReturn(config);
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(creatorId);
    ExternalUser admin = user(UUID.randomUUID(), "admin");
    when(userClientService.resolveAdmins()).thenReturn(List.of(admin));
    when(userClientService.getUser(creatorId)).thenReturn(user(creatorId, "service.head"));
    notificationService.notifyValidationReminder(incident, 48);
    assertThat(captureNotification().getRecipient()).isEqualTo("admin");
  }

  @Test
  void ownServiceCreatorValidationReminderReachesHeadWhenEnabled() {
    UUID serviceId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    incident.setCreatedBy(creatorId);
    incident.setTypeId(UUID.randomUUID());
    IncidentTypeConfig config = new IncidentTypeConfig();
    config.setDefaultTargetServiceId(serviceId);
    when(incidentTypeConfigService.findById(incident.getTypeId())).thenReturn(config);
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(creatorId);
    when(systemConfig.getThresholdLong("serviceManagerSelfValidationEnabled", 0)).thenReturn(1L);
    when(userClientService.getUser(creatorId)).thenReturn(user(creatorId, "service.head"));
    notificationService.notifyValidationReminder(incident, 48);
    assertThat(captureNotification().getRecipient()).isEqualTo("service.head");
  }

  private NotificationClientRequest captureNotification() {
    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient).createNotification(captor.capture());
    return captor.getValue();
  }

  // Type d'incident dont le service cible pilote la validation (scope TARGET).
  private void configureTargetService(UUID targetServiceId) {
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig config = new IncidentTypeConfig();
    config.setDefaultTargetServiceId(targetServiceId);
    when(incidentTypeConfigService.findById(typeId)).thenReturn(config);
  }

  // Active les e-mails pour le type de l'incident de test.
  private void enableEmailNotifications() {
    configureNotifications(true, true);
  }

  // Configure les anciens indicateurs de canaux pour verifier leur compatibilite.
  private void configureNotifications(boolean email, boolean inApp) {
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig config = new IncidentTypeConfig();
    config.setEmailNotificationsEnabled(email);
    config.setInAppNotificationsEnabled(inApp);
    when(incidentTypeConfigService.findById(typeId)).thenReturn(config);
  }

  // Verifie la repartition exacte des notifications par canal.
  private void assertNotificationChannels(int internalCount, int emailCount) {
    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient, times(internalCount + emailCount))
      .createNotification(captor.capture());
    assertThat(
      captor.getAllValues().stream().filter(r -> "INTERNAL".equals(r.getType()))
    ).hasSize(internalCount);
    assertThat(
      captor.getAllValues().stream().filter(r -> "EMAIL".equals(r.getType()))
    ).hasSize(emailCount);
  }

  @Test
  @DisplayName("notifySlaBreached - Sends internal and email notifications to all concerned parties")
  void notifySlaBreached_SendsBothChannels() {
    UUID creatorId = UUID.randomUUID();
    UUID assigneeId = UUID.randomUUID();
    UUID creatorServiceId = UUID.randomUUID();
    UUID targetServiceId = UUID.randomUUID();
    UUID creatorServiceHeadId = UUID.randomUUID();
    UUID targetServiceHeadId = UUID.randomUUID();
    UUID agencyHeadId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();

    incident.setCreatedBy(creatorId);
    incident.setAssignedTo(assigneeId);
    incident.setCreatorServiceId(creatorServiceId);
    incident.setTransferredToService(targetServiceId);
    incident.setStatus(IncidentStatus.IN_PROGRESS);

    enableEmailNotifications();

    when(userClientService.getUser(creatorId)).thenReturn(user(creatorId, "creator"));
    when(userClientService.getUser(assigneeId)).thenReturn(user(assigneeId, "assignee"));
    when(userClientService.resolveAgencyHead(incident.getAgencyId()))
      .thenReturn(agencyHeadId);
    when(userClientService.getUser(agencyHeadId))
      .thenReturn(user(agencyHeadId, "agency.head"));
    when(userClientService.resolveServiceHead(creatorServiceId))
      .thenReturn(creatorServiceHeadId);
    when(userClientService.getUser(creatorServiceHeadId))
      .thenReturn(user(creatorServiceHeadId, "creator.service.head"));
    when(userClientService.resolveServiceHead(targetServiceId))
      .thenReturn(targetServiceHeadId);
    when(userClientService.getUser(targetServiceHeadId))
      .thenReturn(user(targetServiceHeadId, "target.service.head"));
    when(userClientService.resolveAdmins())
      .thenReturn(List.of(user(adminId, "admin")));

    notificationService.notifySlaBreached(incident, "En cours");

    ArgumentCaptor<NotificationClientRequest> captor = ArgumentCaptor.forClass(
      NotificationClientRequest.class
    );
    verify(notificationClient, times(12)).createNotification(captor.capture());

    List<NotificationClientRequest> internalReqs = captor.getAllValues().stream()
      .filter(r -> "INTERNAL".equals(r.getType())).toList();
    List<NotificationClientRequest> emailReqs = captor.getAllValues().stream()
      .filter(r -> "EMAIL".equals(r.getType())).toList();

    assertThat(internalReqs).hasSize(6);
    assertThat(emailReqs).hasSize(6);

    assertThat(internalReqs.stream().map(NotificationClientRequest::getRecipient))
      .containsExactlyInAnyOrder(
        "creator",
        "assignee",
        "agency.head",
        "creator.service.head",
        "target.service.head",
        "admin"
      );
    assertThat(emailReqs.stream().map(NotificationClientRequest::getRecipient))
      .containsExactlyInAnyOrder(
        "creator@fintrack.test",
        "assignee@fintrack.test",
        "agency.head@fintrack.test",
        "creator.service.head@fintrack.test",
        "target.service.head@fintrack.test",
        "admin@fintrack.test"
      );
  }

  @Test
  @DisplayName(
    "notifyLateIncidentsDailyReport - Builds a translated and enriched CSV attachment"
  )
  void notifyLateIncidentsDailyReport_BuildsTranslatedCsv() {
    UUID adminId = UUID.randomUUID();
    UUID assigneeId = UUID.randomUUID();

    incident.setReference("FT-I-2026-0001");
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setCriticality(Criticality.CRITICAL);
    incident.setAssignedTo(assigneeId);
    incident.setCreatedAt(LocalDateTime.of(2026, 8, 3, 11, 36));
    incident.setDueDate(LocalDateTime.of(2026, 8, 4, 18, 0));

    when(userClientService.resolveAdmins()).thenReturn(
      List.of(user(adminId, "admin"))
    );
    when(userClientService.getUser(assigneeId)).thenReturn(
      ExternalUser.builder()
        .id(assigneeId)
        .username("jdupont")
        .firstName("Jean")
        .lastName("Dupont")
        .email("jdupont@fintrack.test")
        .build()
    );
    when(
      messageSource.getMessage(
        eq("enum.incident_status.ASSIGNED.name"),
        any(),
        anyString(),
        eq(Locale.FRENCH)
      )
    ).thenReturn("Assigné");
    when(
      messageSource.getMessage(
        eq("enum.criticality.CRITICAL.name"),
        any(),
        anyString(),
        eq(Locale.FRENCH)
      )
    ).thenReturn("Critique");

    notificationService.notifyLateIncidentsDailyReport(List.of(incident));

    NotificationClientRequest request = captureNotification();
    assertThat(request.getTemplateParams())
      .containsEntry("report_count", 1)
      .containsKey("attachment_content");

    String csv = new String(
      Base64.getDecoder()
        .decode((String) request.getTemplateParams().get("attachment_content")),
      StandardCharsets.UTF_8
    );
    assertThat(csv)
      .contains(
        "Référence;Titre;Statut;Criticité;Agence;Assigné à;Date de création;Échéance"
      )
      .contains("FT-I-2026-0001")
      .contains("Assigné")
      .contains("Critique")
      .contains("Agency Alpha")
      .contains("Jean Dupont")
      .contains("03/08/2026 11:36")
      .contains("04/08/2026 18:00")
      // Les codes techniques bruts ne doivent plus apparaitre a la place des libelles.
      .doesNotContain("\"ASSIGNED\"")
      .doesNotContain("\"CRITICAL\"");
  }

  // Construit un utilisateur minimal joignable par notification interne.
  private ExternalUser user(UUID id, String username) {
    return ExternalUser.builder()
      .id(id)
      .username(username)
      .email(username + "@fintrack.test")
      .build();
  }
}
