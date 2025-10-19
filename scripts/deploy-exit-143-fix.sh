#!/bin/bash

# ==============================================================================
# Quick Fix Deployment Script - Exit Code 143 Issue
# ==============================================================================
# This script rebuilds the Docker image with critical fixes and deploys to K8s
# ==============================================================================

set -e

VERSION="v1.2.7.3"
IMAGE_NAME="sreerajrone/exploresg-fleet-service"
NAMESPACE="exploresg"
DEPLOYMENT="exploresg-fleet-service"

echo "🔧 Deploying Fleet Service Fix for Exit Code 143"
echo "=================================================="
echo ""

# Step 1: Build Docker Image
echo "📦 Step 1: Building Docker image..."
docker build -t ${IMAGE_NAME}:${VERSION} .
echo "✅ Image built successfully"
echo ""

# Step 2: Push to Registry
echo "🚀 Step 2: Pushing image to Docker Hub..."
docker push ${IMAGE_NAME}:${VERSION}
echo "✅ Image pushed successfully"
echo ""

# Step 3: Update Kubernetes Deployment
echo "☸️  Step 3: Updating Kubernetes deployment..."
kubectl set image deployment/${DEPLOYMENT} \
  exploresg-fleet-service=${IMAGE_NAME}:${VERSION} \
  -n ${NAMESPACE}

echo "✅ Deployment updated"
echo ""

# Step 4: Monitor Rollout
echo "👀 Step 4: Monitoring rollout status..."
kubectl rollout status deployment/${DEPLOYMENT} -n ${NAMESPACE} --timeout=5m

echo ""
echo "✅ Deployment completed successfully!"
echo ""

# Step 5: Verify Pod Status
echo "🔍 Step 5: Verifying pod status..."
kubectl get pods -n ${NAMESPACE} | grep ${DEPLOYMENT}
echo ""

# Step 6: Show Recent Events
echo "📋 Recent events:"
kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp' | grep ${DEPLOYMENT} | tail -10
echo ""

echo "=========================================="
echo "✅ Fix deployment complete!"
echo ""
echo "Monitor with:"
echo "  kubectl logs -f deployment/${DEPLOYMENT} -n ${NAMESPACE}"
echo "  kubectl get pods -n ${NAMESPACE} -w | grep ${DEPLOYMENT}"
echo ""
echo "Expected: Pod should start and stay running (no restarts)"
echo "=========================================="
