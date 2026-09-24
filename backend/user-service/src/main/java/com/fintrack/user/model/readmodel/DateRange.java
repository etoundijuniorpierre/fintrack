// Composant backend : porte la logique liee a date range.

package com.fintrack.user.model.readmodel;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte la responsabilite applicative liee a date range.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateRange {

  private LocalDateTime from;
  private LocalDateTime to;
}
