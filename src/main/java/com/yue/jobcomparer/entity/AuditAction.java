package com.yue.jobcomparer.entity;

public enum AuditAction {
    // Security events
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    REGISTER,

    // Business resource changes
    CV_CREATE,
    CV_UPDATE,
    CV_DELETE,

    JOB_CREATE,
    JOB_UPDATE,
    JOB_STATUS_UPDATE,
    JOB_DELETE,

    ANALYSIS_CREATE,
    ANALYSIS_DELETE
}
