package com.healthvault.platform.schedule;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled}. Without this, scheduled methods never fire. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
