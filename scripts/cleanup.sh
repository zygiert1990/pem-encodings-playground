#!/bin/bash

set -e

# Define directories (relative to script location)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
RESOURCES_DIR="$PROJECT_ROOT/src/main/resources"
NGINX_DIR="$PROJECT_ROOT/nginx-server"

echo "=== Cleaning up nginx mTLS setup ==="
echo ""

# Stop and remove Docker container
if [ "$(docker ps -aq -f name=nginx-mtls)" ]; then
    echo "Stopping and removing nginx-mtls container..."
    docker stop nginx-mtls 2>/dev/null || true
    docker rm nginx-mtls 2>/dev/null || true
    echo "✓ Container removed"
else
    echo "No nginx-mtls container found"
fi
echo ""

# Remove certificates from resources directory
if [ -d "$RESOURCES_DIR" ]; then
    echo "Removing certificates from $RESOURCES_DIR..."
    rm -f "$RESOURCES_DIR/ca-key.pem"
    rm -f "$RESOURCES_DIR/ca-cert.pem"
    rm -f "$RESOURCES_DIR/ca-cert.srl"
    rm -f "$RESOURCES_DIR/client-key.pem"
    rm -f "$RESOURCES_DIR/client-cert.pem"
    rm -f "$RESOURCES_DIR/client.csr"
    echo "✓ Resources certificates removed"
else
    echo "Resources directory not found"
fi
echo ""

# Remove nginx-server directory entirely
if [ -d "$NGINX_DIR" ]; then
    echo "Removing nginx-server directory: $NGINX_DIR..."
    rm -rf "$NGINX_DIR"
    echo "✓ nginx-server directory removed"
else
    echo "nginx-server directory not found"
fi
echo ""

echo "=== Cleanup complete! ==="
echo ""
echo "To recreate everything, run:"
echo "  ./scripts/generate-certs.sh"
echo "  ./scripts/start-nginx.sh"