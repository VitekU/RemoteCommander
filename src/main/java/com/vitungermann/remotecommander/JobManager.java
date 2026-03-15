package com.vitungermann.remotecommander;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.vitungermann.remotecommander.helperstructs.JobResponse;
import com.vitungermann.remotecommander.helperstructs.JobStatus;
import com.vitungermann.remotecommander.helperstructs.JobOperationResponse;
import com.vitungermann.remotecommander.helperstructs.ServiceStatus;
import org.springframework.beans.factory.annotation.Value;
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

    private ServiceStatus status;

    @Value("${docker.image}")
    private String dockerImage;

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
        JobOperationResponse response = startContainer();
        if (!response.isSuccessful()) {
            return;
        }

        Job job = allJobs.get(currentJobID);

        response = executeCommand();
        job.operationResponses.add(response.message());

        if (!response.isSuccessful()) {
            job.status = JobStatus.FAILED;
        }
        else {
            job.status = JobStatus.FINISHED;
        }
        dockerManager.dockerClient.stopContainerCmd(job.containerID).exec();
        currentJobID = null;
        executeJob();
    }

    private JobOperationResponse executeCommand() {
        try {
            ExecCreateCmdResponse exec = dockerManager.dockerClient.execCreateCmd(allJobs.get(currentJobID).containerID).withAttachStdout(true).withAttachStderr(true).withCmd("sh", "-c", allJobs.get(currentJobID).command).exec();
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            dockerManager.dockerClient.execStartCmd(exec.getId()).exec(new ExecStartResultCallback(stdout, stderr)).awaitCompletion();
            allJobs.get(currentJobID).output = stdout.toString();

            return new JobOperationResponse(true, "Command executed successfully.");
        }
        catch (Exception e) {
            return new JobOperationResponse(false, "Command could not be executed: " + e.getMessage());
        }
    }

    private JobOperationResponse startContainer() {
        if (currentJobID != null && allJobs.get(currentJobID).status == JobStatus.IN_PROGRESS) {
            return new JobOperationResponse(false, "There is another job in progress.");
        }

        currentJobID = jobQueue.poll();

        if (currentJobID == null) {
            return new JobOperationResponse(false, "You have no jobs in queue right now.");
        }

        Job job = allJobs.get(currentJobID);
        try {

            HostConfig hostConfig = HostConfig.newHostConfig().withCpuCount(job.cpuCount).withMemory(job.memorySize * 1024 * 1024L).withMemorySwap(job.memorySize * 1024 * 1024L).withRestartPolicy(RestartPolicy.noRestart())
                    .withAutoRemove(true);

            CreateContainerResponse container = dockerManager.dockerClient.createContainerCmd(dockerImage).withName("executor-" + job.jobID).withHostConfig(hostConfig).withCmd("sh", "-c", "sleep infinity").exec();
            dockerManager.dockerClient.startContainerCmd(container.getId()).exec();

            job.containerID = container.getId();
            job.status = JobStatus.IN_PROGRESS;

            return new JobOperationResponse(true, "New container started successfully.");
        }
        catch (Exception e) {
            job.status = JobStatus.FAILED;
            this.status = ServiceStatus.ERROR;
            return new JobOperationResponse(false, "Container could not be started: " + e.getMessage());
        }
    }

    public List<JobResponse> getAllJobs() {
        List<JobResponse> output = new ArrayList<>();
        for (Job job : allJobs.values()) {
            output.add(new JobResponse(job.jobID, job.command, job.status, job.output, job.cpuCount, job.memorySize, job.operationResponses));
        }
        return output;
    }

    public JobResponse getJob(String id) {
        Job job = allJobs.get(id);
        if (job == null) {
            return new JobResponse(null, null, null, null, 0,0, Collections.emptyList());
        }
        return new JobResponse(job.jobID, job.command, job.status, job.output, job.cpuCount, job.memorySize, job.operationResponses);
    }

    public ServiceStatus getStatus() {
        return status;
    }

    JobManager(DockerManager dockerManager) {
        this.dockerManager = dockerManager;
        this.jobQueue = new LinkedList<>();
        this.allJobs = new HashMap<>();
        this.status = ServiceStatus.OK;
    }
}
