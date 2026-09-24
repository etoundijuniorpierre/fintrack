// DTO : transporte les donnees liees a notifications page client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a notifications page client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsPageClientResponse {

  private List<NotificationClientResponse> content;
  private long totalElements;
  private int totalPages;
  private int size;
  private int number;
}
