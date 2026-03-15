package com.vitungermann.remotecommander;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
class JobManager {
    private final DockerManager dockerManager;

    private final Queue<Job> jobQueue;
    private final HashMap<String, Job> allJobs;
    public Job currentJob;

    public Job enqueueJob(String command, long cpuCount, long memorySize) {
        Job newJob = new Job(command, cpuCount, memorySize, JobStatus.QUEUED);
        jobQueue.add(newJob);


        CompletableFuture.runAsync(() -> {
            try {
                executeJob();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        return newJob;
    }

    private void executeJob() throws InterruptedException {
        JobResponse response = startContainer();
        if (!response.isReady()) {
            return;
        }
        executeCommand();

        currentJob.status = JobStatus.FINISHED;
        dockerManager.dockerClient.stopContainerCmd(currentJob.containerID).exec();

        currentJob = null;
        executeJob();
    }

    private void executeCommand() throws InterruptedException {
        ExecCreateCmdResponse exec = dockerManager.dockerClient.execCreateCmd(currentJob.containerID).withAttachStdout(true).withAttachStderr(true).withCmd("sh", "-c", currentJob.command).exec();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        dockerManager.dockerClient.execStartCmd(exec.getId()).exec(new ExecStartResultCallback(stdout, stderr)).awaitCompletion();
        currentJob.output = stdout.toString();
    }

    private JobResponse startContainer() {
        if (currentJob != null && currentJob.status == JobStatus.IN_PROGRESS) {
            return new JobResponse(false, "There is another job in progress.");
        }

        currentJob = jobQueue.poll();

        if (currentJob == null) {
            return new JobResponse(false, "You have no jobs in queue right now.");
        }

        HostConfig hostConfig = HostConfig.newHostConfig().withCpuCount(currentJob.cpuCount).withMemory(currentJob.memorySize * 1024 * 1024L).withMemorySwap(currentJob.memorySize * 1024 * 1024L).withRestartPolicy(RestartPolicy.noRestart())
                .withAutoRemove(true);

        CreateContainerResponse container = dockerManager.dockerClient.createContainerCmd("ubuntu").withName("executor" + currentJob.jobID).withHostConfig(hostConfig).withCmd("sh", "-c", "sleep infinity").exec();
        dockerManager.dockerClient.startContainerCmd(container.getId()).exec();

        currentJob.containerID = container.getId();
        currentJob.status = JobStatus.IN_PROGRESS;
        return new JobResponse(true, "New container started successfully");
    }

    public Queue<Job> listJobs() {
        return jobQueue;
    }

    JobManager(DockerManager dockerManager) {
        this.dockerManager = dockerManager;
        this.jobQueue = new LinkedList<>();
        this.allJobs = new HashMap<>();
    }
}
