package com.vitungermann.remotecommander;

import com.vitungermann.remotecommander.helperstructs.JobStatus;

import java.util.ArrayList;
import java.util.List;

public class Job {
    public String jobID;
    public String containerID;
    public String command;
    public long cpuCount;
    public long memorySize;
    public JobStatus status;
    public String output;
    public List<String> operationResponses;

    public Job(String jobID,String command, long cpuCount, long memorySize, JobStatus jobStatus) {
        this.jobID = jobID;
        this.command = command;
        this.cpuCount = cpuCount;
        this.memorySize = memorySize;
        this.status = jobStatus;
        this.output = "";
        operationResponses = new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format("%s %s %d %d %s %s", this.jobID, this.command, this.cpuCount, this.memorySize, this.status, this.output);
    }
}
