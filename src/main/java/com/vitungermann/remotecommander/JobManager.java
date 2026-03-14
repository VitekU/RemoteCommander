package com.vitungermann.remotecommander;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
class JobManager {
    private final DockerManager dockerManager;

    private Queue<Job> jobs;
    public Job currentJob;

    public void createJob(String command, long cpuCount, long memorySize) throws InterruptedException {
        jobs.add(new Job(command, cpuCount, memorySize, JobStatus.QUEUED));
        startContainer();
        executeCommand();
    }

    private void executeCommand() throws InterruptedException {
        ExecCreateCmdResponse exec = dockerManager.dockerClient.execCreateCmd(currentJob.containerID).withAttachStdout(true).withAttachStderr(true).withCmd("sh", "-c", currentJob.command).exec();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        dockerManager.dockerClient.execStartCmd(exec.getId()).exec(new ExecStartResultCallback(stdout, stderr)).awaitCompletion();

        currentJob.output = stdout.toString();
        currentJob.status = JobStatus.FINISHED;
        dockerManager.dockerClient.stopContainerCmd(currentJob.containerID).exec();
    }

    private boolean startContainer() {
        if (currentJob != null && currentJob.status == JobStatus.IN_PROGRESS) {
            return false;
        }

        currentJob = jobs.poll();

        if (currentJob == null) {
            return false;
        }
        HostConfig hostConfig = HostConfig.newHostConfig().withCpuCount(currentJob.cpuCount).withMemory(currentJob.memorySize * 1024 * 1024L).withMemorySwap(currentJob.memorySize * 1024 * 1024L).withRestartPolicy(RestartPolicy.noRestart())
                .withAutoRemove(true);

        CreateContainerResponse container = dockerManager.dockerClient.createContainerCmd("ubuntu").withName("executor" + (int)(Math.random() * 100)).withHostConfig(hostConfig).withCmd("sh", "-c", "sleep infinity").exec();
        dockerManager.dockerClient.startContainerCmd(container.getId()).exec();

        currentJob.containerID = container.getId();
        currentJob.status = JobStatus.IN_PROGRESS;
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
