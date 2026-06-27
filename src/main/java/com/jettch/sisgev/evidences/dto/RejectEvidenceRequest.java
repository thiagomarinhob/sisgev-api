package com.jettch.sisgev.evidences.dto;

import jakarta.validation.constraints.NotBlank;

/** Rejeição de evidência exige motivo (vira admin_note). */
public record RejectEvidenceRequest(
        @NotBlank(message = "Motivo da rejeição é obrigatório")
        String reason
) {
}
