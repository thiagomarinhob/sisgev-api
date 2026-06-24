package com.jettch.sisgev.municipalities.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de criação/edição de município.
 * Validações: nome obrigatório; UF com exatamente 2 letras (spec §20.2).
 */
public record MunicipalityRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
        String name,

        @NotBlank(message = "UF é obrigatória")
        @Pattern(regexp = "[A-Za-z]{2}", message = "UF deve ter exatamente 2 letras")
        String state,

        @Size(max = 20, message = "Código IBGE deve ter no máximo 20 caracteres")
        String ibgeCode
) {
}
