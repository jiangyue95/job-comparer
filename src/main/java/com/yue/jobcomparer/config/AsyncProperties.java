package com.yue.jobcomparer.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.async.analysis")
public class AsyncProperties {

    @Positive
    private int corePoolSize = 2;

    @Positive
    private int maxPoolSize = 2;

    @PositiveOrZero
    private int queueCapacity = 50;

    @NotBlank
    private String threadNamePrefix = "analysis-";
}
