package com.vitungermann.remotecommander;

import org.springframework.stereotype.Service;

@Service
class JobManager {
    private final DockerManager dockerManager;


    JobManager(DockerManager dockerManager) {
        this.dockerManager = dockerManager;
    }
}
