#!/usr/bin/env bash
# Create the kind cluster (if needed), build the image, load it into the cluster and deploy.
#   bash scripts/kind-up.sh            # builds password-generator-api:local
#   IMAGE=password-generator-api:b5-3f2a9c1 bash scripts/kind-up.sh   # deploy a Jenkins-built image
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."
CLUSTER=passgen
IMAGE="${IMAGE:-password-generator-api:local}"

if ! kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
    echo "==> creating kind cluster '$CLUSTER'"
    kind create cluster --config kind/kind-config.yaml
fi
kubectl config use-context "kind-$CLUSTER" >/dev/null

if [ "$IMAGE" = "password-generator-api:local" ]; then
    echo "==> building $IMAGE"
    docker build --build-arg BUILD_TAG=local -t "$IMAGE" .
fi

echo "==> loading $IMAGE into the cluster (kind nodes cannot see the laptop's images otherwise)"
kind load docker-image "$IMAGE" --name "$CLUSTER"

echo "==> applying manifests"
kubectl apply -k k8s
if [ "$IMAGE" != "password-generator-api:local" ]; then
    kubectl -n passgen set image deployment/passgen api="$IMAGE"
fi
kubectl -n passgen rollout status deployment/passgen --timeout=180s

kubectl -n passgen get deployments,services,pods -o wide
echo
echo "==> http://localhost:8090/api/password/generate"
curl -s http://localhost:8090/api/password/generate; echo
