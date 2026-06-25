package com.jettch.sisgev.inspections.dto;

/**
 * Resultado do upsert de vistoria. {@code created=false} indica que o registro
 * já existia (idempotência), permitindo ao controller responder 200 em vez de 201.
 */
public record InspectionSyncResult(InspectionResponse inspection, boolean created) {
}
