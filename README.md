# Password Generator API — DevOps PBL (CIAEC59)

A small Spring Boot REST API that generates secure passwords and checks password strength,
taken through a full pipeline: **Git → Maven → Jenkins (+ GitHub Actions) → Ansible →
Docker → Kubernetes (kind)**.

Repository: https://github.com/tonne-omkar/password-generator-devops

## API

| Method | Path | What it does |
|---|---|---|
| GET | `/api/password/generate?length=16&upper=true&lower=true&digits=true&symbols=true&excludeAmbiguous=false` | Random password (Java `SecureRandom`), at least one character of every selected type, plus entropy and a strength label |
| POST | `/api/password/strength` with `{"password": "..."}` | Entropy, score 0–4, label and warnings |
| GET | `/api/password/health` | Simple health and version |
| GET | `/actuator/health/liveness`, `/actuator/health/readiness` | Kubernetes probes |
| GET | `/actuator/prometheus` | Metrics for Prometheus |

Errors come back as HTTP 400 with `{"error": "..."}`. There is no database: the app is stateless,
so any pod can answer any request, which is what makes scaling trivial.

```bash
curl "http://localhost:8081/api/password/generate?length=20"
curl -X POST http://localhost:8081/api/password/strength -H "Content-Type: application/json" -d '{"password":"qwerty"}'
```

## Tools (Windows 11)

```powershell
winget install EclipseAdoptium.Temurin.21.JDK Apache.Maven Docker.DockerDesktop Kubernetes.kubectl Kubernetes.kind GitHub.cli
```
Docker Desktop needs WSL 2 (it offers to enable it). Restart the terminal afterwards. Run the
`.sh` scripts from **Git Bash**.

## Component 1 — Git, Maven, Jenkins

```bash
mvn -B clean verify          # 111 JUnit tests + JaCoCo 80% coverage gate + JAR in target/
java -jar target/password-generator-api-1.0.0.jar
```

**Jenkins** (configuration as code in `jenkins/`):
```bash
docker compose -f jenkins/docker-compose.yml up -d --build
# http://localhost:8080  ->  admin / admin123
```
The job *Password Generator API* is created automatically from `jenkins/casc.yaml`, points at
this repository's `main` branch and polls it every minute (`pollSCM('* * * * *')`). Merge a PR
and a build starts within a minute with nobody pressing *Build Now*.

Pipeline (`Jenkinsfile`): **Version → Test → Build → Docker image**, then archive the JAR.

CI enhancements:
- **Automated tests**: the Test stage runs JUnit and the JaCoCo coverage gate before packaging.
- **Build versioning**: JAR and image are tagged `b<build number>-<commit SHA>`.
- **GitHub Actions**: `.github/workflows/ci.yml` runs the same build on every push and PR.

## Component 2 — Ansible, Docker, Kubernetes

**Ansible** (`ansible/`): a bare Ubuntu 24.04 container is the managed host.
```bash
mvn -B package -DskipTests
docker compose -f ansible/docker-compose.yml up -d --build app-server
docker compose -f ansible/docker-compose.yml run --rm ansible ansible-playbook -i inventory playbook.yml
docker compose -f ansible/docker-compose.yml run --rm ansible ansible-playbook -i inventory playbook.yml   # 2nd run: changed=0
curl http://localhost:8082/api/password/health
```
Role `passgen_app`: installs Java 21, copies the JAR, templates the config and systemd unit.
Variables in `group_vars/app_servers.yml`; handlers restart the service only on change.

**Docker** (multi-stage):
```bash
docker build -t password-generator-api:local .
docker run --rm -p 8081:8081 password-generator-api:local
```

**Kubernetes** on kind:
```bash
bash scripts/kind-up.sh            # cluster + image + kubectl apply -k k8s
kubectl -n passgen get deployments,services,pods -o wide
curl http://localhost:8090/api/password/generate
bash scripts/scale-demo.sh         # 3 -> 5 -> 3 replicas
bash scripts/rollback-demo.sh      # broken config, rollout stalls, kubectl rollout undo
```

Deployment enhancements: **rollback demo**, **ConfigMap** (`k8s/configmap.yaml`: port and length
limits), **liveness/readiness/startup probes**, **multi-stage Dockerfile**, **Prometheus metrics
endpoint**.

ConfigMap demo:
```bash
kubectl -n passgen patch configmap passgen-config -p '{"data":{"PASSGEN_MAX_LENGTH":"32"}}'
kubectl -n passgen rollout restart deployment/passgen && kubectl -n passgen rollout status deployment/passgen
curl "http://localhost:8090/api/password/generate?length=40"   # {"error":"Password length must be between 8 and 32"}
```

## Layout

```
src/                 Spring Boot app and JUnit tests
pom.xml              Maven build (fat JAR, build-info, git info, JaCoCo gate)
Jenkinsfile          CI pipeline
jenkins/             Jenkins in Docker, configured as code
.github/workflows/   GitHub Actions
ansible/             playbook, inventory, group_vars, role passgen_app, Docker lab
Dockerfile           multi-stage image
k8s/                 Namespace, ConfigMap, Deployment, Service, kustomization
kind/                kind cluster config
scripts/             kind-up, scale and rollback demos
repo_link.txt        repository link for submission
```

Jenkins polls GitHub every minute and builds each merge to main automatically.
