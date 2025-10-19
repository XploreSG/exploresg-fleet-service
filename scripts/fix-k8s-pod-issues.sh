#!/bin/bash

# ==============================================================================
# Kubernetes Pod Restart Issue - Quick Fix Script
# ==============================================================================
# Purpose: Resolve fleet service pod restart issues
# Date: October 19, 2025
# ==============================================================================

set -e  # Exit on error

NAMESPACE="exploresg"
DEPLOYMENT="exploresg-fleet-service"
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[1;33m'
COLOR_RED='\033[0;31m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

echo -e "${COLOR_BLUE}======================================${COLOR_RESET}"
echo -e "${COLOR_BLUE}Fleet Service Pod Issue Resolver${COLOR_RESET}"
echo -e "${COLOR_BLUE}======================================${COLOR_RESET}"
echo ""

# Function to print colored output
print_info() {
    echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $1"
}

print_success() {
    echo -e "${COLOR_GREEN}[SUCCESS]${COLOR_RESET} $1"
}

print_warning() {
    echo -e "${COLOR_YELLOW}[WARNING]${COLOR_RESET} $1"
}

print_error() {
    echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $1"
}

# ==============================================================================
# Step 1: Gather Current State
# ==============================================================================
print_info "Step 1: Gathering current state..."
echo ""

print_info "Current pods in namespace:"
kubectl get pods -n $NAMESPACE | grep fleet || true
echo ""

print_info "Current ReplicaSets:"
kubectl get rs -n $NAMESPACE | grep fleet-service || true
echo ""

print_info "Current Deployment:"
kubectl get deployment $DEPLOYMENT -n $NAMESPACE
echo ""

print_info "Deployment image version:"
CURRENT_IMAGE=$(kubectl get deployment $DEPLOYMENT -n $NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].image}')
echo "Image: $CURRENT_IMAGE"
echo ""

# ==============================================================================
# Step 2: Check Pod Health
# ==============================================================================
print_info "Step 2: Checking pod health..."
echo ""

PODS=$(kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT -o jsonpath='{.items[*].metadata.name}')
for POD in $PODS; do
    STATUS=$(kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.status.phase}')
    RESTARTS=$(kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.status.containerStatuses[0].restartCount}')
    READY=$(kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.status.containerStatuses[0].ready}')
    
    echo "Pod: $POD"
    echo "  Status: $STATUS"
    echo "  Ready: $READY"
    echo "  Restarts: $RESTARTS"
    
    if [ "$RESTARTS" -gt 3 ]; then
        print_warning "  ⚠️  High restart count detected!"
    fi
    
    if [ "$READY" != "true" ]; then
        print_warning "  ⚠️  Pod not ready!"
    fi
    echo ""
done

# ==============================================================================
# Step 3: Check Recent Events
# ==============================================================================
print_info "Step 3: Checking recent events..."
echo ""

kubectl get events -n $NAMESPACE --sort-by='.lastTimestamp' | grep fleet | tail -10
echo ""

# ==============================================================================
# Step 4: Provide Recommendations
# ==============================================================================
print_info "Step 4: Analysis and Recommendations"
echo ""

# Count total pods
TOTAL_PODS=$(kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT --no-headers | wc -l)
RUNNING_PODS=$(kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT --field-selector=status.phase=Running --no-headers | wc -l)
DESIRED_REPLICAS=$(kubectl get deployment $DEPLOYMENT -n $NAMESPACE -o jsonpath='{.spec.replicas}')

echo "Desired Replicas: $DESIRED_REPLICAS"
echo "Total Pods: $TOTAL_PODS"
echo "Running Pods: $RUNNING_PODS"
echo ""

if [ "$TOTAL_PODS" -gt "$DESIRED_REPLICAS" ]; then
    print_warning "More pods than desired replicas detected!"
    print_info "This indicates multiple ReplicaSets are active."
    echo ""
fi

# ==============================================================================
# Step 5: Interactive Fix Options
# ==============================================================================
print_info "Step 5: Fix Options"
echo ""

echo "Choose an action:"
echo "1) Delete failing pods and let them restart"
echo "2) Rollback deployment to previous version"
echo "3) Scale down old ReplicaSets"
echo "4) View detailed logs of failing pods"
echo "5) Check deployment rollout status"
echo "6) Exit (no changes)"
echo ""

read -p "Enter your choice (1-6): " choice

case $choice in
    1)
        print_info "Deleting failing pods..."
        for POD in $PODS; do
            RESTARTS=$(kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.status.containerStatuses[0].restartCount}')
            if [ "$RESTARTS" -gt 3 ]; then
                print_warning "Deleting pod: $POD (Restarts: $RESTARTS)"
                kubectl delete pod $POD -n $NAMESPACE
            fi
        done
        print_success "Failed pods deleted. They will be recreated."
        ;;
    
    2)
        print_info "Rolling back deployment..."
        kubectl rollout undo deployment/$DEPLOYMENT -n $NAMESPACE
        print_info "Waiting for rollback to complete..."
        kubectl rollout status deployment/$DEPLOYMENT -n $NAMESPACE
        print_success "Rollback completed!"
        ;;
    
    3)
        print_info "Scaling down old ReplicaSets..."
        OLD_RS=$(kubectl get rs -n $NAMESPACE -l app=$DEPLOYMENT --sort-by=.metadata.creationTimestamp -o jsonpath='{.items[0].metadata.name}')
        if [ ! -z "$OLD_RS" ]; then
            CURRENT_REPLICAS=$(kubectl get rs $OLD_RS -n $NAMESPACE -o jsonpath='{.spec.replicas}')
            if [ "$CURRENT_REPLICAS" -gt 0 ]; then
                print_info "Scaling down ReplicaSet: $OLD_RS"
                kubectl scale rs $OLD_RS --replicas=0 -n $NAMESPACE
                print_success "Old ReplicaSet scaled down!"
            else
                print_info "Old ReplicaSet already scaled down."
            fi
        fi
        ;;
    
    4)
        print_info "Fetching logs from failing pods..."
        for POD in $PODS; do
            RESTARTS=$(kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.status.containerStatuses[0].restartCount}')
            if [ "$RESTARTS" -gt 3 ]; then
                print_info "Logs for $POD:"
                echo "--- Current logs ---"
                kubectl logs $POD -n $NAMESPACE --tail=50 || true
                echo ""
                echo "--- Previous logs ---"
                kubectl logs $POD -n $NAMESPACE --previous --tail=50 2>/dev/null || print_warning "No previous logs available"
                echo ""
            fi
        done
        ;;
    
    5)
        print_info "Checking deployment rollout status..."
        kubectl rollout status deployment/$DEPLOYMENT -n $NAMESPACE
        echo ""
        print_info "Rollout history:"
        kubectl rollout history deployment/$DEPLOYMENT -n $NAMESPACE
        ;;
    
    6)
        print_info "Exiting without changes."
        exit 0
        ;;
    
    *)
        print_error "Invalid choice. Exiting."
        exit 1
        ;;
esac

echo ""
print_info "Checking final state..."
echo ""

kubectl get pods -n $NAMESPACE | grep fleet
echo ""

print_success "Operation completed!"
print_info "Monitor pods with: kubectl get pods -n $NAMESPACE -w | grep fleet"
