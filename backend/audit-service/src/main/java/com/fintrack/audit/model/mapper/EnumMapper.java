// Mapper : convertit les donnees liees a enum entre modeles.

package com.fintrack.audit.model.mapper;

import com.fintrack.audit.model.constant.LocalizableEnum;
import com.fintrack.audit.model.dto.response.EnumResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

// Mapper centralisant la conversion des enumerations metier en EnumResponse traduits.
@Component
@RequiredArgsConstructor
public class EnumMapper {

  private final MessageSource messageSource;

  public <E extends Enum<E> & LocalizableEnum> List<EnumResponse> toResponses(
    E[] values,
    Locale locale
  ) {
    return Arrays.stream(values)
      .map(e -> toResponse(e, locale))
      .toList();
  }

  // Convertit une seule valeur d'enumeration en EnumResponse traduit.
  public EnumResponse toResponse(LocalizableEnum value, Locale locale) {
    return new EnumResponse(
      value.getName(),
      messageSource.getMessage(
        value.getNameKey(),
        null,
        value.getName(),
        locale
      ),
      messageSource.getMessage(
        value.getDescriptionKey(),
        null,
        value.getName(),
        locale
      )
    );
  }
}
