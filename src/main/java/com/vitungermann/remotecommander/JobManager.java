package com.vitungermann.remotecommander;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.vitungermann.remotecommander.helperstructs.JobResponse;
import com.vitungermann.remotecommander.helperstructs.JobStatus;
import com.vitungermann.remotecommander.helperstructs.StartJobResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
class JobManager {
    private final DockerManager dockerManager;

    private final Queue<String> jobQueue;
    private final HashMap<String, Job> allJobs;
    private String currentJobID;


    public Job enqueueJob(String command, long cpuCount, long memorySize) {
        String newJobID = UUID.randomUUID().toString();
        Job newJob = new Job(newJobID, command, cpuCount, memorySize, JobStatus.QUEUED);
        jobQueue.add(newJobID);
        allJobs.put(newJobID, newJob);


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
        StartJobResponse response = startContainer();
        if (!response.isReady()) {
            return;
        }
        executeCommand();

        allJobs.get(currentJobID).status = JobStatus.FINISHED;
        dockerManager.dockerClient.stopContainerCmd(allJobs.get(currentJobID).containerID).exec();

        currentJobID = null;
        executeJob();
    }

    private void executeCommand() throws InterruptedException {
        ExecCreateCmdResponse exec = dockerManager.dockerClient.execCreateCmd(allJobs.get(currentJobID).containerID).withAttachStdout(true).withAttachStderr(true).withCmd("sh", "-c", allJobs.get(currentJobID).command).exec();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        dockerManager.dockerClient.execStartCmd(exec.getId()).exec(new ExecStartResultCallback(stdout, stderr)).awaitCompletion();
        allJobs.get(currentJobID).output = stdout.toString();
    }

    private StartJobResponse startContainer() {
        if (currentJobID != null && allJobs.get(currentJobID).status == JobStatus.IN_PROGRESS) {
            return new StartJobResponse(false, "There is another job in progress.");
        }

        currentJobID = jobQueue.poll();

        if (currentJobID == null) {
            return new StartJobResponse(false, "You have no jobs in queue right now.");
        }

        HostConfig hostConfig = HostConfig.newHostConfig().withCpuCount(allJobs.get(currentJobID).cpuCount).withMemory(allJobs.get(currentJobID).memorySize * 1024 * 1024L).withMemorySwap(allJobs.get(currentJobID).memorySize * 1024 * 1024L).withRestartPolicy(RestartPolicy.noRestart())
                .withAutoRemove(true);

        CreateContainerResponse container = dockerManager.dockerClient.createContainerCmd("ubuntu").withName("executor-" + allJobs.get(currentJobID).jobID).withHostConfig(hostConfig).withCmd("sh", "-c", "sleep infinity").exec();
        dockerManager.dockerClient.startContainerCmd(container.getId()).exec();

        allJobs.get(currentJobID).containerID = container.getId();
        allJobs.get(currentJobID).status = JobStatus.IN_PROGRESS;
        return new StartJobResponse(true, "New container started successfully");
    }

    public List<JobResponse> getAllJobs() {
        List<JobResponse> output = new ArrayList<>();
        for (Job job : allJobs.values()) {
            output.add(new JobResponse(job.jobID, job.command, job.status, job.output, job.cpuCount, job.memorySize));
        }
        return output;
    }

    public JobResponse getJob(String id) {
        Job job = allJobs.get(id);
        if (job == null) {
            return new JobResponse(null, null, null, null, 0,0);
        }
        return new JobResponse(job.jobID, job.command, job.status, job.output, job.cpuCount, job.memorySize);
    }

    JobManager(DockerManager dockerManager) {
        this.dockerManager = dockerManager;
        this.jobQueue = new LinkedList<>();
        this.allJobs = new HashMap<>();
    }
}
