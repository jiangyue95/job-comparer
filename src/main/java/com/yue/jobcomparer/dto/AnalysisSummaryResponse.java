package com.yue.jobcomparer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisSummaryResponse {

    /**
     * Terminal analyses the user has not opened yet. Drives the unread badge.
     */
    private long unread;

    /**
     * Analyses still queued or running. Drive the client's polling interval.
     */
    private long active;
}
