# GitHub Actions Setup

This document explains how to configure repository secrets and runners for the CI/CD pipelines.

1. Add repository secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `REGISTRY_URL`, `CLOUD_PROVIDER_CREDENTIALS`.
2. Ensure secrets for DB and JWT are configured for deployment jobs.
3. See `.github/workflows/ci-java.yml` for the CI pipeline.
