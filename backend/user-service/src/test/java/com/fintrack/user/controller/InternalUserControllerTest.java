package com.fintrack.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.user.model.dto.response.UserInternalResponse;
import com.fintrack.user.model.entity.Agency;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.mapper.UserInternalMapper;
import com.fintrack.user.service.AgencyService;
import com.fintrack.user.service.DepartmentService;
import com.fintrack.user.service.UserService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalUserControllerTest {

  @Mock
  private UserService userService;

  @Mock
  private AgencyService agencyService;

  @Mock
  private DepartmentService departmentService;

  @Mock
  private UserInternalMapper userInternalMapper;

  @InjectMocks
  private InternalUserController controller;

  @Test
  void getAgencyHead_UsesUniqueActiveAgencyManagerWhenHeadLinkIsMissing() {
    UUID agencyId = UUID.randomUUID();
    User agencyHead = new User();
    agencyHead.setId(UUID.randomUUID());
    agencyHead.setActive(true);
    Role role = new Role();
    role.setName("CHEF_AGENCE");
    agencyHead.setRoles(Set.of(role));

    Agency agency = new Agency();
    agency.setId(agencyId);
    UserInternalResponse response = new UserInternalResponse();
    response.setId(agencyHead.getId());

    when(agencyService.findById(agencyId)).thenReturn(agency);
    when(userService.findByAgencyId(agencyId)).thenReturn(List.of(agencyHead));
    when(userInternalMapper.toInternalResponse(agencyHead)).thenReturn(response);

    var result = controller.getAgencyHead(agencyId);

    assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(result.getBody()).isSameAs(response);
    verify(userService).findByAgencyId(eq(agencyId));
  }
}
