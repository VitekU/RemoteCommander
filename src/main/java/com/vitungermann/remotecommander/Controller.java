package com.vitungermann.remotecommander;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class Controller {

    private final JobManager jobManager;

    @GetMapping("/")
    public String index() {
        return "Api here";
    }

    @PostMapping("/createjob")
    public String createJob(@RequestBody CreateJobRequest request) throws InterruptedException {
        Job newJob = jobManager.enqueueJob(request.command(), request.cpuCount(), request.memorySize());
        return newJob.jobID;
    }

    public Controller(JobManager jobManager) {
        this.jobManager = jobManager;
    }

 }
