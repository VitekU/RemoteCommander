package com.vitungermann.remotecommander.helperstructs;

import java.util.List;

public record JobResponse(String jobID, String command, JobStatus status, String output, long cpuCount, long memorySize, List<String> responses) {}