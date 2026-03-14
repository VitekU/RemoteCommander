package com.vitungermann.remotecommander;


public class Job {
    public String containerID;
    public String command;
    public long cpuCount;
    public long memorySize;
    public JobStatus jobStatus;

    public Job(String command, long cpuCount, long memorySize, JobStatus jobStatus) {
        this.command = command;
        this.cpuCount = cpuCount;
        this.memorySize = memorySize;
        this.jobStatus = jobStatus;
    }

    @Override
    public String toString() {
        return String.format("%s %s %d %d %s", this.containerID, this.command, this.cpuCount, this.memorySize, this.jobStatus);
    }
}
