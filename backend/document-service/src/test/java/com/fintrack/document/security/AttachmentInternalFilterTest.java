package com.fintrack.document.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;

class AttachmentInternalFilterTest {
  @AfterEach void clear() { SecurityContextHolder.clearContext(); }
  @Test void correctTokenGrantsOnlyCleanupAuthority() throws Exception {
    var request = new MockHttpServletRequest();
    request.setServletPath("/api/v1/documentService/internal/attachments/cleanup/id");
    request.addHeader("X-Internal-Service-Token", "test-secret");
    var response = new MockHttpServletResponse();
    var chain = mock(FilterChain.class);
    new AttachmentInternalFilter("test-secret").doFilter(request, response, chain);
    assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
      .extracting("authority").containsExactly("ATTACHMENT_CLEANUP_INTERNAL");
    verify(chain).doFilter(request, response);
  }
  @Test void missingWrongAndUnconfiguredTokensAreRejected() throws Exception {
    for (String configured : new String[]{"test-secret", ""}) {
      var request = new MockHttpServletRequest();
      request.setServletPath("/api/v1/documentService/internal/attachments/cleanup/id");
      request.addHeader("X-Internal-Service-Token", "wrong");
      var response = new MockHttpServletResponse();
      var chain = mock(FilterChain.class);
      new AttachmentInternalFilter(configured).doFilter(request, response, chain);
      assertThat(response.getStatus()).isEqualTo(403);
      verifyNoInteractions(chain);
    }
  }
  @Test void tokenDoesNotAuthenticatePublicAttachmentRoutes() throws Exception {
    var request = new MockHttpServletRequest();
    request.setServletPath("/api/v1/documentService/attachments");
    request.addHeader("X-Internal-Service-Token", "test-secret");
    new AttachmentInternalFilter("test-secret").doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
