package com.jettch.sisgev.evidences.enums;

/** Status da evidência (spec §6.2). */
public enum EvidenceStatus {
    PENDING_UPLOAD,
    UPLOADED,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    DUPLICATED,
    INVALID_LOCATION
}
