#!/bin/bash

set -e

# Define directories (relative to script location)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
NGINX_DIR="$PROJECT_ROOT/nginx-server"
NGINX_CERTS_DIR="$NGINX_DIR/certs"
NGINX_CONF="$NGINX_DIR/nginx.conf"

echo "=== Setting up nginx with mTLS ==="
echo ""

# Create nginx-server directory if it doesn't exist
mkdir -p "$NGINX_DIR"

# Create nginx.conf
echo "Creating nginx configuration..."
cat > "$NGINX_CONF" <<'EOF'
events {
    worker_connections 1024;
}

http {
    server {
        listen 443 ssl;
        server_name localhost;

        # Server certificate and key
        ssl_certificate /etc/nginx/certs/server-cert.pem;
        ssl_certificate_key /etc/nginx/certs/server-key.pem;

        # Client certificate verification (mTLS)
        ssl_client_certificate /etc/nginx/certs/ca-cert.pem;
        ssl_verify_client on;
        ssl_verify_depth 2;

        # SSL protocols and ciphers
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;
        ssl_prefer_server_ciphers on;

        # /hello endpoint
        location /hello {
            add_header Content-Type text/plain;
            return 200 'Hello! mTLS connection successful.\nClient DN: $ssl_client_s_dn\n';
        }

        # Default location
        location / {
            return 404 'Not found';
        }
    }
}
EOF
echo "✓ nginx.conf created at $NGINX_CONF"
echo ""

# Check if certificates exist
if [ ! -f "$NGINX_CERTS_DIR/server-cert.pem" ] || [ ! -f "$NGINX_CERTS_DIR/server-key.pem" ] || [ ! -f "$NGINX_CERTS_DIR/ca-cert.pem" ]; then
    echo "ERROR: Certificates not found in $NGINX_CERTS_DIR"
    echo "Please run generate-certs.sh first"
    exit 1
fi
echo "✓ Certificates found"
echo ""

# Stop and remove existing container if it exists
if [ "$(docker ps -aq -f name=nginx-mtls)" ]; then
    echo "Stopping and removing existing nginx-mtls container..."
    docker stop nginx-mtls 2>/dev/null || true
    docker rm nginx-mtls 2>/dev/null || true
    echo "✓ Existing container removed"
    echo ""
fi

# Start nginx container
echo "Starting nginx container..."
docker run -d \
  --name nginx-mtls \
  -p 8443:443 \
  -v "$NGINX_CONF:/etc/nginx/nginx.conf:ro" \
  -v "$NGINX_CERTS_DIR:/etc/nginx/certs:ro" \
  nginx:alpine

echo "✓ nginx container started"
echo ""

# Wait a moment for nginx to start
sleep 2

# Check if container is running
if [ "$(docker ps -q -f name=nginx-mtls)" ]; then
    echo "=== nginx with mTLS is running! ==="
    echo ""
    echo "Container: nginx-mtls"
    echo "Port: 8443"
    echo "Endpoint: https://localhost:8443/hello"
    echo ""
    echo "Test with:"
    echo "  curl --cert $PROJECT_ROOT/src/main/resources/client-cert.pem \\"
    echo "       --key $PROJECT_ROOT/src/main/resources/client-key.pem \\"
    echo "       --pass MyClientPassword123 \\"
    echo "       --cacert $PROJECT_ROOT/src/main/resources/ca-cert.pem \\"
    echo "       https://localhost:8443/hello"
    echo ""
    echo "View logs:"
    echo "  docker logs -f nginx-mtls"
    echo ""
    echo "Stop container:"
    echo "  docker stop nginx-mtls"
else
    echo "ERROR: Container failed to start"
    echo "Check logs with: docker logs nginx-mtls"
    exit 1
fi