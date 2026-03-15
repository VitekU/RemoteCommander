package com.vitungermann.remotecommander;

import com.vitungermann.remotecommander.helperstructs.CreateJobRequest;
import com.vitungermann.remotecommander.helperstructs.JobResponse;
import com.vitungermann.remotecommander.helperstructs.ServiceStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class Controller {

    private final JobManager jobManager;

    @GetMapping("/")
    public ServiceStatus index() {
        return jobManager.getStatus();
    }

    @PostMapping("/create")
    public String createJob(@RequestBody CreateJobRequest request) {
        Job newJob = jobManager.enqueueJob(request.command(), request.cpuCount(), request.memorySize());
        return newJob.jobID;
    }

    @GetMapping("/list")
    public List<JobResponse> listJobs() {
        return jobManager.getAllJobs();
    }

    @GetMapping("/getjob")
    public JobResponse getJob(@RequestParam String id) {
        return jobManager.getJob(id);
    }

    private Controller(JobManager jobManager) {
        this.jobManager = jobManager;
    }
 }
