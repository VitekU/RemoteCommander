package com.vitungermann.remotecommander;

public record CreateJobRequest(
        String command,
        long cpuCount,
        long memorySize
) {}
