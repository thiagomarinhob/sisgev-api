package com.jettch.sisgev.evidences.dto;

/** {@code created=false} indica que a evidência já existia (idempotência) → 200 em vez de 201. */
public record EvidenceUploadResult(EvidenceResponse evidence, boolean created) {
}
