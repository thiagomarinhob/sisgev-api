package com.jettch.sisgev.occurrences.enums;

/** Status da ocorrência (spec §6.4). */
public enum OccurrenceStatus {
    OPEN,
    IN_ANALYSIS,
    SCHEDULED,
    IN_PROGRESS,
    RESOLVED,
    CANCELLED
}
