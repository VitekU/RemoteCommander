package com.vitungermann.remotecommander.helperstructs;

public record JobResponse(String jobID, String command, JobStatus status, String output, long cpuCount, long memorySize) {}