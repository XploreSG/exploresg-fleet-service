# ==============================================================================
# Kubernetes Pod Restart Issue - Quick Fix Script (PowerShell)
# ==============================================================================
# Purpose: Resolve fleet service pod restart issues
# Date: October 19, 2025
# ==============================================================================

$ErrorActionPreference = "Stop"

$NAMESPACE = "exploresg"
$DEPLOYMENT = "exploresg-fleet-service"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Fleet Service Pod Issue Resolver" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] " -ForegroundColor Blue -NoNewline
    Write-Host $Message
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] " -ForegroundColor Green -NoNewline
    Write-Host $Message
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] " -ForegroundColor Yellow -NoNewline
    Write-Host $Message
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "[ERROR] " -ForegroundColor Red -NoNewline
    Write-Host $Message
}

# ==============================================================================
# Step 1: Gather Current State
# ==============================================================================
Write-Info "Step 1: Gathering current state..."
Write-Host ""

Write-Info "Current pods in namespace:"
kubectl get pods -n $NAMESPACE | Select-String "fleet"
Write-Host ""

Write-Info "Current ReplicaSets:"
kubectl get rs -n $NAMESPACE | Select-String "fleet-service"
Write-Host ""

Write-Info "Current Deployment:"
kubectl get deployment $DEPLOYMENT -n $NAMESPACE
Write-Host ""

Write-Info "Deployment image version:"
$CURRENT_IMAGE = kubectl get deployment $DEPLOYMENT -n $NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].image}'
Write-Host "Image: $CURRENT_IMAGE"
Write-Host ""

# ==============================================================================
# Step 2: Check Pod Health
# ==============================================================================
Write-Info "Step 2: Checking pod health..."
Write-Host ""

$PODS = kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT -o jsonpath='{.items[*].metadata.name}'
$POD_ARRAY = $PODS -split ' '

foreach ($POD in $POD_ARRAY) {
    if ([string]::IsNullOrWhiteSpace($POD)) { continue }
    
    $STATUS = kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.status.phase}'
    $RESTARTS = kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.status.containerStatuses[0].restartCount}'
    $READY = kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.status.containerStatuses[0].ready}'
    
    Write-Host "Pod: $POD"
    Write-Host "  Status: $STATUS"
    Write-Host "  Ready: $READY"
    Write-Host "  Restarts: $RESTARTS"
    
    if ([int]$RESTARTS -gt 3) {
        Write-Warning "  ⚠️  High restart count detected!"
    }
    
    if ($READY -ne "true") {
        Write-Warning "  ⚠️  Pod not ready!"
    }
    Write-Host ""
}

# ==============================================================================
# Step 3: Check Recent Events
# ==============================================================================
Write-Info "Step 3: Checking recent events..."
Write-Host ""

kubectl get events -n $NAMESPACE --sort-by='.lastTimestamp' | Select-String "fleet" | Select-Object -Last 10
Write-Host ""

# ==============================================================================
# Step 4: Provide Recommendations
# ==============================================================================
Write-Info "Step 4: Analysis and Recommendations"
Write-Host ""

# Count total pods
$TOTAL_PODS = (kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT --no-headers | Measure-Object).Count
$RUNNING_PODS = (kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT --field-selector=status.phase=Running --no-headers | Measure-Object).Count
$DESIRED_REPLICAS = kubectl get deployment $DEPLOYMENT -n $NAMESPACE -o jsonpath='{.spec.replicas}'

Write-Host "Desired Replicas: $DESIRED_REPLICAS"
Write-Host "Total Pods: $TOTAL_PODS"
Write-Host "Running Pods: $RUNNING_PODS"
Write-Host ""

if ($TOTAL_PODS -gt [int]$DESIRED_REPLICAS) {
    Write-Warning "More pods than desired replicas detected!"
    Write-Info "This indicates multiple ReplicaSets are active."
    Write-Host ""
}

# ==============================================================================
# Step 5: Interactive Fix Options
# ==============================================================================
Write-Info "Step 5: Fix Options"
Write-Host ""

Write-Host "Choose an action:"
Write-Host "1) Delete failing pods and let them restart"
Write-Host "2) Rollback deployment to previous version"
Write-Host "3) Scale down old ReplicaSets"
Write-Host "4) View detailed logs of failing pods"
Write-Host "5) Check deployment rollout status"
Write-Host "6) Exit (no changes)"
Write-Host ""

$choice = Read-Host "Enter your choice (1-6)"

switch ($choice) {
    "1" {
        Write-Info "Deleting failing pods..."
        foreach ($POD in $POD_ARRAY) {
            if ([string]::IsNullOrWhiteSpace($POD)) { continue }
            
            $RESTARTS = kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.status.containerStatuses[0].restartCount}'
            if ([int]$RESTARTS -gt 3) {
                Write-Warning "Deleting pod: $POD (Restarts: $RESTARTS)"
                kubectl delete pod $POD -n $NAMESPACE
            }
        }
        Write-Success "Failed pods deleted. They will be recreated."
    }
    
    "2" {
        Write-Info "Rolling back deployment..."
        kubectl rollout undo deployment/$DEPLOYMENT -n $NAMESPACE
        Write-Info "Waiting for rollback to complete..."
        kubectl rollout status deployment/$DEPLOYMENT -n $NAMESPACE
        Write-Success "Rollback completed!"
    }
    
    "3" {
        Write-Info "Scaling down old ReplicaSets..."
        $OLD_RS = kubectl get rs -n $NAMESPACE -l app=$DEPLOYMENT --sort-by=.metadata.creationTimestamp -o jsonpath='{.items[0].metadata.name}'
        if (-not [string]::IsNullOrWhiteSpace($OLD_RS)) {
            $CURRENT_REPLICAS = kubectl get rs $OLD_RS -n $NAMESPACE -o jsonpath='{.spec.replicas}'
            if ([int]$CURRENT_REPLICAS -gt 0) {
                Write-Info "Scaling down ReplicaSet: $OLD_RS"
                kubectl scale rs $OLD_RS --replicas=0 -n $NAMESPACE
                Write-Success "Old ReplicaSet scaled down!"
            } else {
                Write-Info "Old ReplicaSet already scaled down."
            }
        }
    }
    
    "4" {
        Write-Info "Fetching logs from failing pods..."
        foreach ($POD in $POD_ARRAY) {
            if ([string]::IsNullOrWhiteSpace($POD)) { continue }
            
            $RESTARTS = kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.status.containerStatuses[0].restartCount}'
            if ([int]$RESTARTS -gt 3) {
                Write-Info "Logs for $POD:"
                Write-Host "--- Current logs ---"
                kubectl logs $POD -n $NAMESPACE --tail=50
                Write-Host ""
                Write-Host "--- Previous logs ---"
                try {
                    kubectl logs $POD -n $NAMESPACE --previous --tail=50 2>$null
                } catch {
                    Write-Warning "No previous logs available"
                }
                Write-Host ""
            }
        }
    }
    
    "5" {
        Write-Info "Checking deployment rollout status..."
        kubectl rollout status deployment/$DEPLOYMENT -n $NAMESPACE
        Write-Host ""
        Write-Info "Rollout history:"
        kubectl rollout history deployment/$DEPLOYMENT -n $NAMESPACE
    }
    
    "6" {
        Write-Info "Exiting without changes."
        exit 0
    }
    
    default {
        Write-Error-Custom "Invalid choice. Exiting."
        exit 1
    }
}

Write-Host ""
Write-Info "Checking final state..."
Write-Host ""

kubectl get pods -n $NAMESPACE | Select-String "fleet"
Write-Host ""

Write-Success "Operation completed!"
Write-Info "Monitor pods with: kubectl get pods -n $NAMESPACE -w | Select-String fleet"
