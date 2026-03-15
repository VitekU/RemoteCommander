package com.vitungermann.remotecommander;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JobManagerTest {

    private JobManager jobManager;
    private DockerManager dockerManager;

    @BeforeEach
    void setUp() {
        jobManager = new JobManager(dockerManager);
    }

    @Test
    void shouldCreateJob() {
        Job job = jobManager.enqueueJob("ls", 2, 512);

        assertEquals("ls", job.command);
        assertEquals(2, job.cpuCount);
        assertEquals(512, job.memorySize);
    }

    @Test
    void shouldAddJobToAllJobs() {
        jobManager.enqueueJob("ls", 2, 512);
        assertEquals(1, jobManager.getAllJobs().size());
    }
}
