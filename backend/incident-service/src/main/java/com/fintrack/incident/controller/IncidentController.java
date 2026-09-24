// Controleur REST : expose les operations HTTP liees a incident.

package com.fintrack.incident.controller;

import com.fintrack.incident.constant.ApiConstants;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentAction;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.dto.request.*;
import com.fintrack.incident.model.dto.response.IncidentResponse;
import com.fintrack.incident.model.dto.response.IncidentSummaryResponse;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.mapper.IncidentMapper;
import com.fintrack.incident.model.readmodel.ExpectedValidator;
import com.fintrack.incident.report.IncidentReportGenerator;
import com.fintrack.incident.security.IncidentAccessGuard;
import com.fintrack.incident.security.IncidentWorkflowGuard;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Controleur REST principal pilotant tout le cycle de vie des incidents (creation, validation, transfert, resolution, etc.)
@RestController
@RequestMapping(ApiConstants.Endpoints.INCIDENTS)
@Tag(
  name = "Incident Management",
  description = "Endpoints for managing incidents"
)
@RequiredArgsConstructor
public class IncidentController {

  private final IncidentService incidentService;
  private final IncidentMapper incidentMapper;
  private final IncidentAccessGuard incidentAccessGuard;
  private final IncidentWorkflowGuard incidentWorkflowGuard;
  private final IncidentReportGenerator incidentReportGenerator;

  @GetMapping
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY', 'INCIDENT_VIEW_SERVICE', 'INCIDENT_VIEW_OWN', 'INCIDENT_TREAT', 'INCIDENT_RESOLVE')"
  )
  @Operation(summary = "Get filtered incidents with pagination")
  // Fournit global incidents au cas d usage appelant.
  public ResponseEntity<Page<IncidentSummaryResponse>> getAllIncidents(
    @RequestParam(required = true) String view,
    @RequestParam(required = false) Set<IncidentStatus> status,
    @RequestParam(required = false) Set<UUID> type,
    @RequestParam(required = false) Set<Criticality> criticality,
    @RequestParam(required = false) UUID assignedTo,
    @RequestParam(required = false) Boolean unassignedOnly,
    @RequestParam(required = false) UUID createdBy,
    @RequestParam(required = false) UUID subjectUserId,
    @RequestParam(required = false) UUID agencyId,
    @RequestParam(required = false) UUID serviceId,
    @RequestParam(required = false) Set<UUID> subjectUserIds,
    @RequestParam(required = false) Set<UUID> agencyIds,
    @RequestParam(required = false) Set<UUID> serviceIds,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE
    ) LocalDate startDate,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE
    ) LocalDate endDate,
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) String missingField,
    @RequestParam(required = false) Boolean assignedToInactive,
    @AuthenticationPrincipal UserDetailsImpl currentUser,
    Pageable pageable
  ) {
    // Verifie que l'utilisateur a la permission correspondant a la vue demandee
    checkViewPermission(view, currentUser);
    checkExplicitScopePermission(
      agencyId,
      serviceId,
      agencyIds,
      serviceIds,
      currentUser
    );

    IncidentSearchRequest searchRequest = IncidentSearchRequest.builder()
      .view(view)
      .statuses(status)
      .typeIds(type)
      .criticalities(criticality)
      .assignedTo(assignedTo)
      .unassignedOnly(unassignedOnly)
      .createdBy(createdBy)
      .subjectUserId(subjectUserId)
      .agencyId(agencyId)
      .serviceId(serviceId)
      .subjectUserIds(subjectUserIds)
      .agencyIds(agencyIds)
      .serviceIds(serviceIds)
      .startDate(startDate)
      .endDate(endDate)
      .keyword(keyword)
      .missingAgency("agency".equals(missingField))
      .missingService("service".equals(missingField))
      .assignedToInactive(assignedToInactive)
      .build();

    Page<Incident> incidents = incidentService.findFiltered(
      incidentMapper.toSearchCriteria(searchRequest),
      currentUser,
      pageable
    );
    List<IncidentSummaryResponse> content =
      incidentMapper.toSummaryResponseList(incidents.getContent());
    return ResponseEntity.ok(
      new PageImpl<>(content, pageable, incidents.getTotalElements())
    );
  }

  // Associe chaque vue (own/assigned/agency/service/all) a la permission requise et refuse l'acces sinon
  private void checkViewPermission(String view, UserDetailsImpl user) {
    String normalizedView = view == null ? "" : view.toLowerCase(Locale.ROOT);
    boolean authorized = false;
    if ("own".equals(normalizedView)) {
      authorized = hasAuthority(user, "INCIDENT_VIEW_OWN");
    } else if ("assigned".equals(normalizedView)) {
      authorized =
        hasAuthority(user, "INCIDENT_TREAT") ||
        hasAuthority(user, "INCIDENT_RESOLVE");
    } else if ("agency".equals(normalizedView)) {
      authorized = hasAuthority(user, "INCIDENT_VIEW_AGENCY");
    } else if ("service".equals(normalizedView)) {
      authorized = hasAuthority(user, "INCIDENT_VIEW_SERVICE");
    } else if ("all".equals(normalizedView)) {
      authorized = hasAuthority(user, "INCIDENT_VIEW_ALL");
    }

    if (!authorized) {
      throw new AccessDeniedException(
        "Vous n'avez pas la permission d'acceder a cette vue : " + view
      );
    }
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  private boolean hasAuthority(UserDetailsImpl user, String authority) {
    return user
      .getAuthorities()
      .stream()
      .anyMatch(a -> authority.equalsIgnoreCase(a.getAuthority()));
  }

  // Realise l'intention metier check explicit scope permission.

  private void checkExplicitScopePermission(
    UUID agencyId,
    UUID serviceId,
    Set<UUID> agencyIds,
    Set<UUID> serviceIds,
    UserDetailsImpl user
  ) {
    boolean explicitScope =
      agencyId != null ||
      serviceId != null ||
      (agencyIds != null && !agencyIds.isEmpty()) ||
      (serviceIds != null && !serviceIds.isEmpty());
    if (explicitScope && !hasAuthority(user, "INCIDENT_VIEW_ALL")) {
      throw new AccessDeniedException(
        "You do not have permission to filter incidents by explicit agency or service"
      );
    }
  }

  @GetMapping("/{id}")
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY', 'INCIDENT_VIEW_SERVICE', 'INCIDENT_VIEW_OWN', 'INCIDENT_TREAT', 'INCIDENT_RESOLVE')"
  )
  @Operation(summary = "Get incident by ID")
  // Fournit incident by identifiant au cas d usage appelant.
  public ResponseEntity<IncidentResponse> getIncidentById(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident incident = incidentService.findById(id);
    assertCanViewIncident(incident, currentUser);
    return ResponseEntity.ok(toResponseWithSolutionVisibility(incident, currentUser));
  }

  @GetMapping("/by-reference/{reference}")
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY', 'INCIDENT_VIEW_SERVICE', 'INCIDENT_VIEW_OWN', 'INCIDENT_TREAT', 'INCIDENT_RESOLVE')"
  )
  @Operation(summary = "Get incident by business reference (FT-I-2026-0001)")
  // Fournit un incident a partir de son code metier lisible
  public ResponseEntity<IncidentResponse> getIncidentByReference(
    @PathVariable String reference,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident incident = incidentService.findByReference(reference);
    assertCanViewIncident(incident, currentUser);
    return ResponseEntity.ok(toResponseWithSolutionVisibility(incident, currentUser));
  }

  @GetMapping("/{id}/report")
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY', 'INCIDENT_VIEW_SERVICE', 'INCIDENT_VIEW_OWN', 'INCIDENT_TREAT', 'INCIDENT_RESOLVE')"
  )
  @Operation(summary = "Download the incident treatment report (PDF)")
  // Produit la fiche de traitement PDF de l'incident (cloture / conformite).
  public ResponseEntity<byte[]> downloadIncidentReport(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser,
    Locale locale
  ) {
    Incident incident = incidentService.findById(id);
    assertCanViewIncident(incident, currentUser);

    byte[] pdf = incidentReportGenerator.generate(
      incidentMapper.toResponse(incident),
      locale
    );

    String filename = slugifyTitle1(incident.getTitle(), id) + ".pdf";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", filename);
    headers.setContentLength(pdf.length);
    return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
  }

  private String slugifyTitle1(String title, UUID id) {
    if (title != null && !title.isBlank()) {
      String slug = Normalizer.normalize(title, Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .trim()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-+|-+$", "");
      if (!slug.isBlank()) {
        return slug;
      }
    }
    return "INC-" + id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
  }

  // Verifie que les regles metier autorisent l'operation sur incident.
  private void assertCanViewIncident(Incident incident, UserDetailsImpl user) {
    incidentAccessGuard.assertCanView(incident, user);
  }

  // Mappe l'incident puis masque la proposition de solution (et le motif de rejet
  // Direction associe) aux utilisateurs hors du cercle autorise a la consulter.
  private IncidentResponse toResponseWithSolutionVisibility(
    Incident incident,
    UserDetailsImpl user
  ) {
    IncidentResponse response = incidentMapper.toResponse(incident);
    if (!incidentWorkflowGuard.canViewProposedSolution(incident, user)) {
      response.setProposedSolution(null);
      response.setDirectionRejectionReason(null);
    }
    response.setCanValidate(incidentWorkflowGuard.canValidate(incident, user));
    response.setCanCancel(incidentWorkflowGuard.canCancel(incident, user));
    response.setCanRequestConfirmation(
      incidentWorkflowGuard.canRequestConfirmation(incident, user)
    );
    response.setCanConfirmRelevance(
      incidentWorkflowGuard.canConfirmRelevance(incident, user)
    );
    // Comme pour le valideur attendu : on ne resout (et on n'appelle) que la ou la
    // question se pose, c'est-a-dire tant que l'incident est en attente prolongee.
    if (incident.getStatus() == IncidentStatus.UNRESOLVED_PROLONGED_WAIT) {
      ExpectedValidator responder =
        incidentWorkflowGuard.resolveRelevanceResponder(incident);
      response.setRelevanceResponderRole(responder.getRole());
      response.setRelevanceResponderTarget(responder.getTargetName());
    }
    // Le valideur attendu n'a de sens (et ne merite ses appels de resolution) que
    // tant que l'incident attend effectivement une validation.
    if (incident.getStatus() == IncidentStatus.PENDING_VALIDATION) {
      ExpectedValidator expected = incidentWorkflowGuard.resolveExpectedValidator(
        incident
      );
      response.setExpectedValidatorRole(expected.getRole());
      response.setExpectedValidatorTarget(expected.getTargetName());
    }
    return response;
  }


  @PostMapping
  @PreAuthorize("hasAuthority('INCIDENT_CREATE')")
  @Operation(summary = "Create a new incident")
  // Prepare l'ajout de incident apres validation metier.
  public ResponseEntity<IncidentResponse> createIncident(
    @Valid @RequestBody IncidentRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    boolean requiresCreatorValidation = !hasAuthority(
      currentUser,
      "INCIDENT_CREATE_OPEN"
    );
    boolean isGlobalAdmin =
      currentUser.getAgencyId() == null &&
      hasAuthority(currentUser, "INCIDENT_VIEW_ALL");
    UUID agencyId = isGlobalAdmin
      ? request.getAgencyId()
      : currentUser.getAgencyId();

    Incident entity = incidentMapper.toEntity(request);
    // Service de rattachement du createur permet la validation par le chef de service.
    entity.setCreatorServiceId(currentUser.getServiceId());

    if (request.isAssignToSelf()) {
      boolean canSelfAssign =
        hasAuthority(currentUser, "INCIDENT_TREAT") ||
        hasAuthority(currentUser, "INCIDENT_RESOLVE");
      if (!canSelfAssign) {
        throw new AccessDeniedException(
          "You do not have permission to self-assign an incident"
        );
      }
    }

    return new ResponseEntity<>(
      incidentMapper.toResponse(
        incidentService.create(
          entity,
          currentUser.getId(),
          agencyId,
          requiresCreatorValidation,
          request.getTargetServiceId(),
          request.isAssignToSelf()
        )
      ),
      HttpStatus.CREATED
    );
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('INCIDENT_UPDATE')")
  @Operation(summary = "Update an existing incident")
  // Applique une modification contrelee sur incident.
  public ResponseEntity<IncidentResponse> updateIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentUpdateRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    // Controle de perimetre : on ne modifie que les incidents que l'on peut voir.
    Incident incident = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      incident,
      currentUser,
      IncidentAction.UPDATE
    );

    Incident target = new Incident();
    incidentMapper.updateFromRequest(request, target);

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.update(id, target, currentUser.getId())
      )
    );
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('INCIDENT_DELETE')")
  @Operation(summary = "Delete an incident")
  // Retire les incidents cibles apres contrele metier.
  public ResponseEntity<Void> deleteIncident(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident incident = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      incident,
      currentUser,
      IncidentAction.DELETE
    );
    incidentService.delete(id, currentUser.getId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/validate")
  @PreAuthorize("hasAuthority('INCIDENT_VALIDATE')")
  @Operation(summary = "Validate an incident")
  // Verifie que les regles metier autorisent l'operation sur incident.
  public ResponseEntity<IncidentResponse> validateIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentValidateRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Set<String> permissions = currentUser
      .getAuthorities()
      .stream()
      .map(a -> a.getAuthority())
      .collect(Collectors.toSet());
    Incident guardedIncident = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      guardedIncident,
      currentUser,
      IncidentAction.VALIDATE
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.validate(
          id,
          currentUser.getId(),
          request.getComment(),
          request.getTargetServiceId(),
          request.getTargetUserId(),
          permissions
        )
      )
    );
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAuthority('INCIDENT_REJECT')")
  @Operation(summary = "Reject an incident")
  // Realise l'intention metier reject incident.
  public ResponseEntity<IncidentResponse> rejectIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentRejectRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    // Controle de perimetre : on ne rejette que les incidents de son perimetre
    // (meme garde que la validation, pour eviter un rejet inter-agences).
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      target,
      currentUser,
      IncidentAction.REJECT
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.reject(
          id,
          currentUser.getId(),
          request.getReason(),
          request.getComment()
        )
      )
    );
  }

  @RequestMapping(
    value = "/{id}/transfer",
    method = { RequestMethod.POST, RequestMethod.PATCH }
  )
  @PreAuthorize("hasAuthority('INCIDENT_TRANSFER')")
  @Operation(summary = "Transfer an incident to another service or to the creator's agency")
  // Realise l'intention metier transfer incident.
  public ResponseEntity<IncidentResponse> transferIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentTransferRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Set<String> permissions = currentUser
      .getAuthorities()
      .stream()
      .map(a -> a.getAuthority())
      .collect(Collectors.toSet());
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanTransfer(
      target,
      currentUser,
      request.getTargetServiceId(),
      request.getTargetAgencyId(),
      request.getReason()
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.transfer(
          id,
          currentUser.getId(),
          request.getTargetServiceId(),
          request.getTargetAgencyId(),
          request.getNewTypeId(),
          request.getReason(),
          request.getComment(),
          permissions
        )
      )
    );
  }

  @PostMapping("/{id}/assign")
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_ASSIGN', 'INCIDENT_TREAT', 'INCIDENT_RESOLVE')"
  )
  @Operation(
    summary = "Assign an incident to a user (self-assign allowed for handlers; assigning others requires INCIDENT_ASSIGN)"
  )
  // Affecte incident au perimetre demande.
  public ResponseEntity<IncidentResponse> assignIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentAssignRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAssign(
      target,
      currentUser,
      request.getAssignedTo()
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.assign(
          id,
          currentUser.getId(),
          request.getAssignedTo(),
          request.getComment()
        )
      )
    );
  }

  @PostMapping("/{id}/start")
  @PreAuthorize("hasAuthority('INCIDENT_TREAT')")
  @Operation(summary = "Start handling an incident")
  // Realise l'intention metier start incident.
  public ResponseEntity<IncidentResponse> startIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentStartRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      target,
      currentUser,
      IncidentAction.START
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.startProgress(
          id,
          currentUser.getId(),
          request.getComment(),
          request.getEstimatedResolutionHours()
        )
      )
    );
  }

  @PostMapping("/{id}/submit-solution")
  @PreAuthorize("hasAuthority('INCIDENT_TREAT')")
  @Operation(summary = "Submit a solution proposal for prior Direction validation")
  public ResponseEntity<IncidentResponse> submitSolution(
    @PathVariable UUID id,
    @Valid @RequestBody SolutionProposalRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.submitSolution(
          id,
          request.getProposedSolution(),
          request.getEstimatedResolutionHours(),
          currentUser
        )
      )
    );
  }

  @PostMapping("/{id}/direction-validate")
  @PreAuthorize(
    "hasAnyAuthority('VALIDATION_DIRECTION', 'INCIDENT_VIEW_ALL')"
  )
  @Operation(summary = "Direction validation of proposed solution")
  public ResponseEntity<IncidentResponse> validateDirection(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.validateDirection(id, currentUser)
      )
    );
  }

  @PostMapping("/{id}/direction-reject")
  @PreAuthorize(
    "hasAnyAuthority('VALIDATION_DIRECTION', 'INCIDENT_VIEW_ALL')"
  )
  @Operation(summary = "Direction rejection of proposed solution")
  public ResponseEntity<IncidentResponse> rejectDirection(
    @PathVariable UUID id,
    @Valid @RequestBody DirectionRejectionRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.rejectDirection(
          id,
          request.getRejectionReason(),
          currentUser
        )
      )
    );
  }

  // Permission dediee (accordee par defaut a tous, revocable). Le perimetre (statut Assigne,
  // chef de service / agence / admin) reste verifie par assertCanCancel.
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('INCIDENT_CANCEL')")
  @Operation(
    summary = "Cancel an assigned incident before handling starts"
  )
  // Verifie que les regles metier autorisent l'operation sur incident.
  public ResponseEntity<IncidentResponse> cancelIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentCancelRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      target,
      currentUser,
      IncidentAction.CANCEL
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.cancel(id, currentUser.getId(), request.getReason())
      )
    );
  }

  // Permission dediee (accordee par defaut a tous, revocable par utilisateur). Le perimetre
  // acteur precis (treaterType) reste verifie par IncidentWorkflowGuard.assertCanTreat.
  @PostMapping("/{id}/treat")
  @PreAuthorize("hasAuthority('INCIDENT_TREAT')")
  @Operation(
    summary = "Mark an in-progress incident as treated (pending resolution validation)"
  )
  // Marque un incident comme traite (IN_PROGRESS -> TREATED).
  public ResponseEntity<IncidentResponse> treatIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentTreatRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      target,
      currentUser,
      IncidentAction.TREAT
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.treat(
          id,
          currentUser.getId(),
          request.getTreatmentDescription(),
          request.getCause(),
          request.getCauseDetail()
        )
      )
    );
  }

  // Permission dediee (accordee par defaut a tous, revocable par utilisateur). Le perimetre
  // acteur precis (resolverType) reste verifie par IncidentWorkflowGuard.assertCanResolve.
  @PostMapping("/{id}/resolve")
  @PreAuthorize("hasAuthority('INCIDENT_RESOLVE')")
  @Operation(summary = "Validate the resolution of a treated incident")
  // Valide la resolution d'un incident traite (TREATED -> RESOLVED).
  public ResponseEntity<IncidentResponse> resolveIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentResolveRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      target,
      currentUser,
      IncidentAction.RESOLVE
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.resolve(
          id,
          currentUser.getId(),
          request.getResolutionNote()
        )
      )
    );
  }

  // Meme permission dediee que la validation de resolution (resolverType affine le perimetre).
  @PostMapping("/{id}/mark-unresolved")
  @PreAuthorize("hasAuthority('INCIDENT_RESOLVE')")
  @Operation(
    summary = "Return a treated incident to processing (resolution judged insufficient)"
  )
  // Renvoie un incident traite en traitement (TREATED -> IN_PROGRESS).
  public ResponseEntity<IncidentResponse> markIncidentUnresolved(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentUnresolvedRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      target,
      currentUser,
      IncidentAction.MARK_UNRESOLVED
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.markUnresolved(
          id,
          currentUser.getId(),
          request.getReason()
        )
      )
    );
  }

  @PostMapping("/{id}/block")
  @PreAuthorize("hasAuthority('INCIDENT_TREAT')")
  @Operation(summary = "Block an incident with a reason")
  // Realise l'intention metier block incident.
  public ResponseEntity<IncidentResponse> blockIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentBlockRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      target,
      currentUser,
      IncidentAction.BLOCK
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.block(
          id,
          currentUser.getId(),
          request.getReason(),
          request.getComment()
        )
      )
    );
  }

  @PostMapping("/{id}/resume")
  @PreAuthorize("hasAuthority('INCIDENT_TREAT')")
  @Operation(summary = "Resume a blocked incident")
  // Realise l'intention metier resume incident.
  public ResponseEntity<IncidentResponse> resumeIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentResumeRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      target,
      currentUser,
      IncidentAction.RESUME
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.resume(id, currentUser.getId(), request.getComment())
      )
    );
  }

  // Attente prolongee : le traitant reprend l'incident, mais l'entite source doit
  // d'abord confirmer qu'il est encore d'actualite. Meme permission que la reprise
  // d'un blocage : c'est le meme geste de traitement.
  @PostMapping("/{id}/request-confirmation")
  @PreAuthorize("hasAuthority('INCIDENT_TREAT')")
  @Operation(
    summary = "Ask the source entity whether a prolonged-wait incident is still relevant"
  )
  public ResponseEntity<IncidentResponse> requestRelevanceConfirmation(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentRelevanceRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      target,
      currentUser,
      IncidentAction.REQUEST_CONFIRMATION
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.requestConfirmation(
          id,
          currentUser.getId(),
          request.getComment()
        )
      )
    );
  }

  // L'entite source confirme l'actualite : l'incident repart en traitement. L'infirmer
  // se fait par l'annulation, qui exige un motif.
  @PostMapping("/{id}/confirm-relevance")
  @PreAuthorize("hasAuthority('INCIDENT_VALIDATE')")
  @Operation(summary = "Confirm a prolonged-wait incident is still relevant")
  public ResponseEntity<IncidentResponse> confirmRelevance(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentRelevanceRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      target,
      currentUser,
      IncidentAction.CONFIRM_RELEVANCE
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.confirmRelevance(
          id,
          currentUser.getId(),
          request.getComment()
        )
      )
    );
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasAuthority('INCIDENT_CLOSE')")
  @Operation(summary = "Close an incident")
  // Realise l'intention metier close incident.
  public ResponseEntity<IncidentResponse> closeIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentCloseRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident target = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      target,
      currentUser,
      IncidentAction.CLOSE
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.close(
          id,
          currentUser.getId(),
          request.getClosureDescription(),
          request.getComment()
        )
      )
    );
  }

  @PostMapping("/{id}/reopen")
  @PreAuthorize("hasAuthority('INCIDENT_REOPEN')")
  @Operation(
    summary = "Reopen a resolved or rejected incident (CLOSED cannot be reopened)"
  )
  // Realise l'intention metier reopen incident.
  public ResponseEntity<IncidentResponse> reopenIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentReopenRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident incident = incidentService.findById(id);
    incidentWorkflowGuard.assertCanReopen(
      incident,
      currentUser,
      request.getReason()
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.reopen(
          id,
          currentUser.getId(),
          request.getReason(),
          request.getComment()
        )
      )
    );
  }

  @PostMapping("/{id}/clone")
  @PreAuthorize("hasAuthority('INCIDENT_CREATE')")
  @Operation(summary = "Clone an incident")
  // Realise l'intention metier clone incident.
  public ResponseEntity<IncidentResponse> cloneIncident(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident incident = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      incident,
      currentUser,
      IncidentAction.CLONE
    );

    return new ResponseEntity<>(
      incidentMapper.toResponse(
        incidentService.cloneIncident(id, currentUser.getId())
      ),
      HttpStatus.CREATED
    );
  }

  @PostMapping("/{id}/resubmit")
  @PreAuthorize("hasAuthority('INCIDENT_CREATE')")
  @Operation(summary = "Resubmit a reopened incident")
  // Realise l'intention metier resubmit incident.
  public ResponseEntity<IncidentResponse> resubmitIncident(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentValidateRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident incident = incidentService.findById(id);
    incidentWorkflowGuard.assertCanAct(
      incident,
      currentUser,
      IncidentAction.RESUBMIT
    );

    boolean requiresCreatorValidation = !hasAuthority(
      currentUser,
      "INCIDENT_CREATE_OPEN"
    );

    return ResponseEntity.ok(
      incidentMapper.toResponse(
        incidentService.resubmit(
          id,
          currentUser.getId(),
          request.getComment(),
          requiresCreatorValidation
        )
      )
    );
  }
}
