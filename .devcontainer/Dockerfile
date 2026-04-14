# Dev Container workspace image for OnlineShop.
# Provides JDK 25, Node LTS, Docker CLI, Claude Code CLI, and common dev tools.
# Spring Boot services and the React frontend are run from inside this container
# against infra (Postgres, Redis, Kafka) running as sibling Compose services.
FROM mcr.microsoft.com/devcontainers/java:1-21-bookworm

# --- JDK 25 (Temurin) -------------------------------------------------------
# Base image ships with 21; we need 25 to match the services' pom.xml.
# Installed via the devcontainer "java" feature at devcontainer.json level,
# which layers on top of this image and sets JAVA_HOME.

# --- OS tooling -------------------------------------------------------------
# The base image ships with a stale Yarn apt source whose GPG key has expired,
# which breaks `apt-get update`. We don't use Yarn (we use npm), so we drop
# the Yarn repo entirely before updating the package index.
RUN rm -f /etc/apt/sources.list.d/yarn.list \
    && apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        curl \
        jq \
        git \
        bash-completion \
        postgresql-client \
        redis-tools \
        netcat-openbsd \
        unzip \
        procps \
    && rm -rf /var/lib/apt/lists/*

# --- Node LTS + Claude Code CLI --------------------------------------------
# Node is installed via the devcontainer "node" feature (see devcontainer.json).
# Claude Code CLI is installed in postCreateCommand so that the image build
# stays cacheable and the CLI can be updated without rebuilding the image.

# --- Non-root user ----------------------------------------------------------
# Base image already provides the "vscode" user (uid 1000).
USER vscode
WORKDIR /workspaces/OnlineShop-claude
