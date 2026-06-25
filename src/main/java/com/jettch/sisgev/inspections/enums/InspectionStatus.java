package com.jettch.sisgev.inspections.enums;

/** Status da vistoria (spec §6.3). */
public enum InspectionStatus {
    DRAFT,
    IN_PROGRESS,
    PENDING_SYNC,
    SYNCED,
    SYNC_ERROR,
    CLOSED,
    CANCELLED
}
