package com.vitungermann.remotecommander;

public class Job {
    private String containerID;
    private String command;

    private int cpuCount;
    private int memorySize;
    private JobStatus jobStatus;


    public Job(String containerID, String command, int cpuCount, int memorySize, JobStatus jobStatus) {
        this.containerID = containerID;
        this.command = command;
        this.cpuCount = cpuCount;
        this.memorySize = memorySize;
        this.jobStatus = jobStatus;
    }
}
