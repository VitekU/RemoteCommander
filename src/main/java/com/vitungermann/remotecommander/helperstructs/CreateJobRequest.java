package com.vitungermann.remotecommander.helperstructs;

public record CreateJobRequest(
        String command,
        long cpuCount,
        long memorySize
) {}
