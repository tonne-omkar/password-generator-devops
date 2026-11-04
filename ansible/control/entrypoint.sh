#!/bin/sh
# Create the SSH key pair once, in the volume the app server also mounts (it trusts the
# public key through AuthorizedKeysFile), then run whatever command was given.
set -eu

if [ ! -f /keys/id_ed25519 ]; then
    ssh-keygen -q -t ed25519 -N '' -C ansible-control -f /keys/id_ed25519
    echo "Generated SSH key /keys/id_ed25519"
fi
chmod 0600 /keys/id_ed25519
chmod 0644 /keys/id_ed25519.pub

exec "$@"
