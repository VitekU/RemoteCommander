package com.vitungermann.remotecommander;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.*;

@Service
class JobManager {
    private final DockerManager dockerManager;

    private Queue<Job> jobs;
    public Job currentJob;

    public void createJob(String command, long cpuCount, long memorySize) {
        jobs.add(new Job(command, cpuCount, memorySize, JobStatus.QUEUED));
        startContainer();
    }

    private boolean startContainer() {
        if (currentJob.jobStatus == JobStatus.IN_PROGRESS) {
            return false;
        }
        currentJob = jobs.poll();

        if (currentJob == null) {
            return false;
        }
        HostConfig hostConfig = HostConfig.newHostConfig().withCpuCount(currentJob.cpuCount).withMemory(currentJob.memorySize * 1024 * 1024L).withMemorySwap(currentJob.memorySize * 1024 * 1024L).withRestartPolicy(RestartPolicy.noRestart())
                .withAutoRemove(true);

        CreateContainerResponse container = dockerManager.dockerClient.createContainerCmd("ubuntu").withName("executor").withHostConfig(hostConfig).exec();
        dockerManager.dockerClient.startContainerCmd(container.getId()).exec();

        currentJob.containerID = container.getId();
        currentJob.jobStatus = JobStatus.IN_PROGRESS;
        return true;
    }

    public Queue<Job> listJobs() {
        return jobs;
    }

    JobManager(DockerManager dockerManager) {
        this.dockerManager = dockerManager;
        this.jobs = new LinkedList<>();
    }
}
