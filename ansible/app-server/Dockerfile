# A stand-in for a freshly installed Ubuntu server: systemd, SSH and a sudo user called
# "ansible". Deliberately no Python and no Java - the playbook installs those.
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive \
    container=docker

# hadolint ignore=DL3008
RUN apt-get update \
 && apt-get install -y --no-install-recommends systemd systemd-sysv openssh-server sudo ca-certificates \
 && rm -rf /var/lib/apt/lists/* \
 && useradd -m -s /bin/bash ansible \
 && echo 'ansible ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/ansible \
 && chmod 0440 /etc/sudoers.d/ansible \
 && printf 'PasswordAuthentication no\nAuthorizedKeysFile /keys/id_ed25519.pub\n' > /etc/ssh/sshd_config.d/lab.conf \
 && systemctl enable ssh

# The control node generates the key pair into the shared /keys volume.
EXPOSE 22 8081
STOPSIGNAL SIGRTMIN+3
CMD ["/lib/systemd/systemd"]
