#!/usr/bin/env bash
# Scale 3 -> 5 -> 3 and show the pods (and the Service endpoints) at each step.
set -euo pipefail

k() { kubectl -n passgen "$@"; }
log() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }

log "Before scaling"
k get pods -l app=passgen -o wide

log "kubectl scale deployment passgen --replicas=5"
k scale deployment passgen --replicas=5
start=$(date +%s)
k rollout status deployment/passgen --timeout=120s
log "All 5 ready after $(( $(date +%s) - start )) seconds"
k get pods -l app=passgen -o wide

log "The Service now sends traffic to 5 pod IPs"
k get endpoints passgen

log "kubectl scale deployment passgen --replicas=3"
k scale deployment passgen --replicas=3
k get pods -l app=passgen
sleep 15
log "A few seconds later"
k get pods -l app=passgen -o wide
k get svc passgen
