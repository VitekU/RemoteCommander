# REMOTE COMMANDER

## Description
This is my implementation of a simple service that allows the client to execute shell commands remotely on a computer (or in cloud). The service uses Springboot for the API and Docker for the remote execution. 

## Setup
This project uses `docker-java` package to communicate with the docker daemon running on the remote machine.
We need to setup the connection, so first you have to enable your docker daemon to accept traffic via network.

#### Warning!
**Due to simplicity, this setup is supposed to be run only on your local network, where we have control over every device connected to this network. If used in public, secure connection must be established via TLS certificates.**

In order to enable the daemon to communicate over network, we have to modify the Docker service config with the following command `sudo systemctl edit docker`. 
Then we can add the following snippet. 
```
[Service]
ExecStart=
ExecStart=/usr/bin/dockerd -H fd:// -H tcp://0.0.0.0:2375
```
This exposes the docker daemon on port 2375. Now restart the daemon.
```
sudo systemctl daemon-reload
sudo systemctl restart docker
```
Lastly we need to make sure, that we have the correct image that will be used for the remote execution. By default, it's `alpine`, but it is of course possible to exchange for a different image. We can just do 
```
sudo docker pull alpine
```
to pull the latest alpine image.  

Now in the service itself, we need to modify the `application.properties`. We have to change the URL of the computer where is running our docker daemon. If we decided to stick with alpine, we can leave the `docker.image` property untouched, otherwise, we would have to modify it so that it matches the image of our choosing.

## API Endpoints

### `GET /`
Returns the current service status.

**Response:** `ServiceStatus` object
```json
{
    "status": "OK"
}
```

---

### `POST /create`
Creates a new job and adds it to the queue.

**Request body:**
```json
{
    "command": "ls",
    "cpuCount": 2,
    "memorySize": 512
}
```

**Response:** Job ID as a string
```
"550e8400-e29b-41d4-a716-446655440000"
```

---

### `GET /list`
Returns a list of all jobs.

**Response:** Array of job objects
```json
[
  {
    "jobID": "5dbc5b08-4119-4d31-bc31-c7d1df26ecfd",
    "command": "ls",
    "status": "FINISHED",
    "output": "bin\ndev\netc\nhome\nlib\nmedia\nmnt\nopt\nproc\nroot\nrun\nsbin\nsrv\nsys\ntmp\nusr\nvar\n",
    "cpuCount": 2,
    "memorySize": 512,
    "responses": [
      "Command executed successfully."
    ]
  }
]
```

---

### `GET /getjob`
Returns a single job by ID.

**Query parameter:**

| Parameter | Type | Description |
|---|---|---|
| `id` | `String` | The job ID to look up |

**Response:**
```json
{
  "jobID": "5cbdfe9c-c84c-4c0a-a777-a3f737ac9f7a",
  "command": "echo Ahoj svete!",
  "status": "FINISHED",
  "output": "Ahoj svete!\n",
  "cpuCount": 2,
  "memorySize": 512,
  "responses": [
    "Command executed successfully."
  ]
}
```
---

### `GET /restart`
Attempts to execute the next job at the top of the queue. 

**Note:** The service will only try to execute the job when there was an service wide error. Otherwise it was made this way, so that the scheduling of the job execution is entirely up to the service.

**Response:** `JobOperationResponse` object
```json
{"isSuccessful":false,"message":"You can't interfere with the services job scheduling order - only if something has gone wrong with the container startup."}
```
