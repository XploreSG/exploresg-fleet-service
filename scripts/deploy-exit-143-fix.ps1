# ==============================================================================
# Quick Fix Deployment Script - Exit Code 143 Issue (PowerShell)
# ==============================================================================
# This script rebuilds the Docker image with critical fixes and deploys to K8s
# ==============================================================================

$ErrorActionPreference = "Stop"

$VERSION = "v1.2.7.3"
$IMAGE_NAME = "sreerajrone/exploresg-fleet-service"
$NAMESPACE = "exploresg"
$DEPLOYMENT = "exploresg-fleet-service"

Write-Host "🔧 Deploying Fleet Service Fix for Exit Code 143" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build Docker Image
Write-Host "📦 Step 1: Building Docker image..." -ForegroundColor Yellow
docker build -t "${IMAGE_NAME}:${VERSION}" .
Write-Host "✅ Image built successfully" -ForegroundColor Green
Write-Host ""

# Step 2: Push to Registry
Write-Host "🚀 Step 2: Pushing image to Docker Hub..." -ForegroundColor Yellow
docker push "${IMAGE_NAME}:${VERSION}"
Write-Host "✅ Image pushed successfully" -ForegroundColor Green
Write-Host ""

# Step 3: Update Kubernetes Deployment
Write-Host "☸️  Step 3: Updating Kubernetes deployment..." -ForegroundColor Yellow
kubectl set image deployment/$DEPLOYMENT `
  exploresg-fleet-service="${IMAGE_NAME}:${VERSION}" `
  -n $NAMESPACE

Write-Host "✅ Deployment updated" -ForegroundColor Green
Write-Host ""

# Step 4: Monitor Rollout
Write-Host "👀 Step 4: Monitoring rollout status..." -ForegroundColor Yellow
kubectl rollout status deployment/$DEPLOYMENT -n $NAMESPACE --timeout=5m

Write-Host ""
Write-Host "✅ Deployment completed successfully!" -ForegroundColor Green
Write-Host ""

# Step 5: Verify Pod Status
Write-Host "🔍 Step 5: Verifying pod status..." -ForegroundColor Yellow
kubectl get pods -n $NAMESPACE | Select-String $DEPLOYMENT
Write-Host ""

# Step 6: Show Recent Events
Write-Host "📋 Recent events:" -ForegroundColor Yellow
kubectl get events -n $NAMESPACE --sort-by='.lastTimestamp' | Select-String $DEPLOYMENT | Select-Object -Last 10
Write-Host ""

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "✅ Fix deployment complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Monitor with:" -ForegroundColor Yellow
Write-Host "  kubectl logs -f deployment/$DEPLOYMENT -n $NAMESPACE"
Write-Host "  kubectl get pods -n $NAMESPACE -w | Select-String $DEPLOYMENT"
Write-Host ""
Write-Host "Expected: Pod should start and stay running (no restarts)" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
