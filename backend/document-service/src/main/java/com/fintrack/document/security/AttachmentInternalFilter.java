package com.fintrack.document.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AttachmentInternalFilter extends OncePerRequestFilter {
  private final String token;
  public AttachmentInternalFilter(String token) { this.token = token; }
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getServletPath().startsWith("/api/v1/documentService/internal/attachments/")) {
      String supplied = request.getHeader("X-Internal-Service-Token");
      if (token == null || token.isBlank() || supplied == null ||
          !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
        response.sendError(403);
        return;
      }
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        "attachment-cleanup", null, List.of(new SimpleGrantedAuthority("ATTACHMENT_CLEANUP_INTERNAL"))));
    }
    chain.doFilter(request, response);
  }
}
