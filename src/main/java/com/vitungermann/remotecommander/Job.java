package com.vitungermann.remotecommander;


import java.util.UUID;

public class Job {
    public String jobID;
    public String containerID;
    public String command;
    public long cpuCount;
    public long memorySize;
    public JobStatus status;
    public String output;

    public Job(String command, long cpuCount, long memorySize, JobStatus jobStatus) {
        this.command = command;
        this.cpuCount = cpuCount;
        this.memorySize = memorySize;
        this.status = jobStatus;

        this.jobID = UUID.randomUUID().toString();


    }

    @Override
    public String toString() {
        return String.format("%s %s %d %d %s %s", this.jobID, this.command, this.cpuCount, this.memorySize, this.status, this.output);
    }
}
