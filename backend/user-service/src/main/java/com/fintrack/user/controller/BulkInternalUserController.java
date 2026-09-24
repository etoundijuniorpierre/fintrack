package com.fintrack.user.controller;

import com.fintrack.user.model.dto.response.AgencyInternalResponse;
import com.fintrack.user.model.dto.response.ServiceInternalResponse;
import com.fintrack.user.model.dto.response.UserInternalResponse;
import com.fintrack.user.model.mapper.UserInternalMapper;
import com.fintrack.user.service.AgencyService;
import com.fintrack.user.service.DepartmentService;
import com.fintrack.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/userService/internal/bulk")
@RequiredArgsConstructor
public class BulkInternalUserController {

  private final UserService userService;
  private final AgencyService agencyService;
  private final DepartmentService departmentService;
  private final UserInternalMapper userInternalMapper;

  @PostMapping("/users")
  @Operation(
    summary = "Get multiple users by IDs",
    description = "Endpoint interne pour éviter les appels N+1"
  )
  public ResponseEntity<List<UserInternalResponse>> getUsersByIds(
    @RequestBody Set<UUID> ids
  ) {
    return ResponseEntity.ok(
      userService
        .findAllByIds(ids)
        .stream()
        .map(userInternalMapper::toInternalResponse)
        .toList()
    );
  }

  @PostMapping("/agencies")
  @Operation(
    summary = "Get multiple agencies by IDs",
    description = "Endpoint interne pour eviter les appels N+1"
  )
  public ResponseEntity<List<AgencyInternalResponse>> getAgenciesByIds(
    @RequestBody Set<UUID> ids
  ) {
    return ResponseEntity.ok(
      agencyService
        .findAllByIds(ids)
        .stream()
        .map(userInternalMapper::toAgencyInternalResponse)
        .toList()
    );
  }

  @PostMapping("/departments")
  @Operation(
    summary = "Get multiple departments by IDs",
    description = "Endpoint interne pour eviter les appels N+1"
  )
  public ResponseEntity<List<ServiceInternalResponse>> getDepartmentsByIds(
    @RequestBody Set<UUID> ids
  ) {
    return ResponseEntity.ok(
      departmentService
        .findAllByIds(ids)
        .stream()
        .map(userInternalMapper::toServiceInternalResponse)
        .toList()
    );
  }
}
