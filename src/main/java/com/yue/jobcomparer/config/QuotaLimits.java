package com.yue.jobcomparer.config;

import lombok.Setter;

@Setter
public class QuotaLimits {

    private Integer maxCvs;
    private Integer maxJobs;
    private Integer dailyAnalyses;

    private boolean unlimited;

    public boolean isUnlimited() {
        return unlimited;
    }

    public int getMaxCvs() {
        return resolve(maxCvs);
    }

    public int getMaxJobs() {
        return resolve(maxJobs);
    }

    public int getDailyAnalyses() {
        return resolve(dailyAnalyses);
    }

    private int resolve(Integer configuredValue) {
        return unlimited ? Integer.MAX_VALUE : configuredValue;
    }

    boolean hasAllLimits() {
        return maxCvs != null && maxJobs != null && dailyAnalyses != null;
    }
}
