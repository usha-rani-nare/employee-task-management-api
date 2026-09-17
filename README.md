# Employee Task Management API

A simple Spring Boot REST API for managing employees and their tasks, built mainly as a way
to actually practice the DevOps/SRE side of things instead of just adding another CRUD app to
my resume. I already had the Java/Spring Boot/MySQL part down from previous projects, so most
of the effort here went into the layer around the application - Docker, CI/CD, Kubernetes, and
monitoring.

## Why this project exists

I was applying for an SRE/DevOps Analyst role and realized my existing projects were all
"build an API" with no story around how it actually gets deployed, monitored, or kept running.
So instead of building another isolated backend project, I containerized this one, wrote a
CI/CD pipeline for it, deployed it with Kubernetes concepts in mind, and set up basic
availability tracking. It's not production-grade infrastructure, but every piece here is
something I actually configured and tested, not just copy-pasted from a tutorial.

## What it does

Basic task management for a small team:

- Create/update/delete employees
- Create/update/delete tasks
- Assign a task to an employee
- Update task status (TODO → IN_PROGRESS → DONE)
- View all tasks, or tasks for a specific employee

Nothing fancy on the application side on purpose - the point of this project is the
infrastructure around it, not the business logic.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3 |
| Database | MySQL 8 |
| API | REST (JSON) |
| Version control | Git + GitHub |
| CI/CD | GitHub Actions |
| Container | Docker (multi-stage build) |
| Cloud | Microsoft Azure (ACR + AKS) |
| Orchestration | Kubernetes |
| Monitoring | Spring Actuator + Azure Monitor |
| Scripting | Python, Bash |
| API testing | Postman |

## Architecture

```
Developer pushes code to GitHub
        │
        ▼
GitHub Actions triggered
        │
        ├─► Checkout code
        ├─► Set up JDK 17
        ├─► Run Maven tests
        ├─► Build Spring Boot jar
        ├─► Build Docker image
        ├─► Push image to Azure Container Registry (ACR)
        └─► Deploy to AKS (kubectl apply + rollout)
```

Inside the cluster:

```
Kubernetes Deployment (employee-api)
    │
    ├── Pod 1 ── Spring Boot container
    ├── Pod 2 ── Spring Boot container
    └── Pod 3 ── Spring Boot container
            │
            ▼
    Service (LoadBalancer) ──► routes traffic across pods
            │
            ▼
    MySQL (external / managed instance)
```

Repo layout:

```
.github/workflows/ci-cd.yml   → CI/CD pipeline
src/                           → application code
Dockerfile                     → multi-stage build
docker-compose.yml             → local dev (app + MySQL together)
k8s/
   deployment.yaml
   service.yaml
   configmap.yaml
   secret.yaml
   hpa.yaml
scripts/
   run-local.sh
   calculate_availability_sli.py
README.md
```

## Running it locally

You need Docker installed, that's really it.

```bash
git clone https://github.com/usharani-nare/employee-task-management-api.git
cd employee-task-management-api
chmod +x scripts/run-local.sh
./scripts/run-local.sh
```

This spins up MySQL and the app together via `docker-compose`. The API will be up at
`http://localhost:8080` once the health check script reports `UP`.

To just build and run the image yourself without compose:

```bash
docker build -t employee-task-manager .
docker run -p 8080:8080 employee-task-manager
```

I tested this by stopping the container and starting it again from the same image
(`docker stop taskmanager-app && docker start taskmanager-app`) to confirm the app comes back
up cleanly from the image without needing a rebuild - that was the whole point of
containerizing it in the first place.

## API endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/employees` | Create employee |
| GET | `/api/employees` | List employees |
| GET | `/api/employees/{id}` | Get one employee |
| PUT | `/api/employees/{id}` | Update employee |
| DELETE | `/api/employees/{id}` | Delete employee |
| POST | `/api/tasks` | Create task |
| GET | `/api/tasks` | List tasks |
| GET | `/api/tasks/{id}` | Get one task |
| PUT | `/api/tasks/{taskId}/assign/{employeeId}` | Assign task to employee |
| PATCH | `/api/tasks/{id}/status` | Update task status |
| GET | `/api/tasks/employee/{employeeId}` | Tasks for a specific employee |
| DELETE | `/api/tasks/{id}` | Delete task |

Health/monitoring:

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | Overall health incl. DB connectivity |
| `/actuator/metrics` | Basic app metrics |

## CI/CD pipeline

The pipeline in `.github/workflows/ci-cd.yml` runs on every push to `main`:

1. **build-and-test** - checks out code, runs `mvn test`, builds the jar
2. **docker-build-push** - builds the Docker image, tags it with the commit SHA (and `latest`),
   pushes both tags to Azure Container Registry
3. **deploy-to-aks** - applies the k8s manifests and updates the deployment to the new image,
   then waits for the rollout to finish

Tagging with the commit SHA (not just `latest`) matters more than I expected once I actually
set this up - if a deploy goes bad you can immediately see which commit's image is running and
roll back to a specific previous SHA instead of guessing.

## Kubernetes

Deployment runs **3 replicas** by default, with readiness and liveness probes both pointed at
`/actuator/health` so Kubernetes actually knows the difference between "container started" and
"container can serve traffic" (important when the app is still connecting to MySQL on startup).

Manual scaling:

```bash
kubectl scale deployment employee-api --replicas=3
```

There's also an `hpa.yaml` that scales between 2 and 5 replicas based on CPU utilization
(target 70%) — this is the automatic version of the manual scaling above. I kept the CPU
threshold simple rather than tuning it, since I don't have real production traffic to base it
on.

## Monitoring & SLI/SLO

I set up Spring Actuator's `/actuator/health` endpoint so it reports database connectivity, not
just "is the JVM alive." In Azure this gets scraped by Azure Monitor for:

- Application availability
- CPU / memory utilization
- Request failures
- Response time
- Application logs

**SLI (Service Level Indicator):** request availability, calculated as

```
Availability = (successful requests / total requests) × 100
```

**SLO (Service Level Objective):** 99% availability

There's a small script at `scripts/calculate_availability_sli.py` that reads a CSV of request
outcomes and reports the availability percentage against the 99% target. It's intentionally
simple - I wanted something I could actually explain line by line, not a monitoring stack I
copied from somewhere.

```bash
python3 scripts/calculate_availability_sli.py scripts/sample_request_log.csv
```

## Incident scenarios (things I actually tested)

### Scenario 1: API returns HTTP 500

```
Alert fires
   ↓
Check /actuator/health → status: DOWN, db: DOWN
   ↓
Check application logs → "Communications link failure"
   ↓
Check MySQL container/pod status → container had stopped
   ↓
Restart MySQL, confirm health endpoint returns UP
   ↓
Re-test API endpoints → 200 OK
```

I simulated this by stopping the MySQL container while the app was running, hitting the API,
and watching it fail with a 500. That's what made me add the global exception handler and the
DB health check in the first place - without it, the raw stack trace goes straight to the
client and you're stuck reading a wall of Spring internals instead of the actual root cause.

### Scenario 2: High CPU / resource usage

```
CPU usage climbs (visible in Azure Monitor / kubectl top)
   ↓
Check pod resource usage → one pod significantly higher than others
   ↓
Check logs for that pod → repeated requests from same pattern
   ↓
Manually scale deployment → kubectl scale deployment employee-api --replicas=5
   ↓
Monitor again → load spreads across new pods, per-pod CPU drops
```

I didn't have real traffic to generate this naturally, so I tested it by hammering one endpoint
with repeated requests locally and watching resource usage climb, then scaling up and watching
it settle. Good enough to explain the mechanics even without a production load generator.

## Things I'd improve with more time

- Actual load testing (JMeter or k6) instead of manually spamming requests
- Centralized logging (right now it's just container logs, no ELK/Log Analytics aggregation)
- Proper secrets management via Azure Key Vault instead of the placeholder `secret.yaml`
- Unit tests beyond the basic ones currently in the repo

## Author

Nare Usha Rani
usharaninare880@gmail.com
