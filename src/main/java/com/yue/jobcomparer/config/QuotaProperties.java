package com.yue.jobcomparer.config;

import com.yue.jobcomparer.entity.Plan;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.quota")
@Setter
public class QuotaProperties {

    private Map<Plan, QuotaLimits> limits = new EnumMap<>(Plan.class);

    public Map<Plan, QuotaLimits> getLimits() {
        return limits;
    }

    public QuotaLimits limitsFor(Plan plan) {
        QuotaLimits result = limits.get(plan);
        if (result == null) {
            throw new IllegalStateException("No quota configured for plan: " + plan);
        }
        return result;
    }

    @PostConstruct
    void validate() {
        for (Plan plan : Plan.values()) {
            QuotaLimits quotaLimits = limits.get(plan);

            if (quotaLimits == null) {
                throw new IllegalStateException(
                        "Missing quota configuration for plan: " + plan
                                + " (expected under app.quota.limits." + plan.name().toLowerCase() + ")");
            }

            if (!quotaLimits.isUnlimited() && !quotaLimits.hasAllLimits()) {
                throw new IllegalStateException(
                        "Incomplete quota configuration for plan: " + plan
                                + " (expected max-cvs, max-jobs and daily-analyses under app.quota.limits."
                                + plan.name().toLowerCase() + ", or unlimited: true)");
            }
        }
    }
}
