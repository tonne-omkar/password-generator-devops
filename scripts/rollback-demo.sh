#!/usr/bin/env bash
# Rollback demo (deployment enhancement).
#
# Ships a broken configuration change - PASSGEN_MIN_LENGTH set to a value that is not a
# number - so the new pods crash at startup. Because the Deployment uses maxUnavailable: 0
# and a readiness probe, the old pods keep serving the whole time. Then
# `kubectl rollout undo` returns to the previous ReplicaSet.
set -euo pipefail

k() { kubectl -n passgen "$@"; }
log() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
health() { printf 'GET /actuator/health -> HTTP %s\n' "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8090/actuator/health)"; }

log "Current state (working version)"
k get deployment passgen -o wide
k get pods -l app=passgen
health

log "Deploying a broken change: PASSGEN_MIN_LENGTH=eight"
k set env deployment/passgen PASSGEN_MIN_LENGTH=eight
k annotate deployment/passgen kubernetes.io/change-cause="demo: broken config PASSGEN_MIN_LENGTH=eight" --overwrite >/dev/null
if k rollout status deployment/passgen --timeout=60s; then
    echo "The broken rollout unexpectedly succeeded" >&2
    exit 1
fi

log "Rollout is stuck: the new pod crashes on startup, the old pods still serve"
k get pods -l app=passgen
health
NEW_POD=$(k get pods -l app=passgen --sort-by=.metadata.creationTimestamp -o name | tail -1)
log "Why the new pod fails ($NEW_POD)"
k logs "$NEW_POD" --tail=200 2>/dev/null | grep -m3 -iE "Failed to bind|PASSGEN|passgen.min-length" || true
k rollout history deployment/passgen

log "Rolling back: kubectl rollout undo deployment/passgen"
k rollout undo deployment/passgen
k rollout status deployment/passgen --timeout=120s

log "Recovered"
k get pods -l app=passgen
health
curl -s http://localhost:8090/api/password/generate; echo
