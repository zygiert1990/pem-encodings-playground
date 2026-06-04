#!/bin/bash

set -e

# Define password for client key only
CLIENT_PASS="MyClientPassword123"

# Define directories (relative to script location)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
RESOURCES_DIR="$PROJECT_ROOT/src/main/resources"
NGINX_CERTS_DIR="$PROJECT_ROOT/nginx-server/certs"

# Create directories if they don't exist
mkdir -p "$RESOURCES_DIR"
mkdir -p "$NGINX_CERTS_DIR"

echo "=== Generating Certificates for mTLS (PEM format) ==="
echo ""
echo "Project structure:"
echo "  Resources dir: $RESOURCES_DIR"
echo "  Nginx certs dir: $NGINX_CERTS_DIR"
echo ""

# Step 1: Generate CA (no password) - stored in resources
echo "Step 1: Generating Certificate Authority (CA) - no password..."
openssl genrsa -out "$RESOURCES_DIR/ca-key.pem" 4096
openssl req -new -x509 -days 365 -key "$RESOURCES_DIR/ca-key.pem" -out "$RESOURCES_DIR/ca-cert.pem" \
  -subj "/C=US/ST=State/L=City/O=Organization/CN=MyCA"
echo "✓ CA certificate generated in resources/"
echo ""

# Step 2: Generate Server Certificate (no password) - stored in nginx-server/certs
echo "Step 2: Generating Server Certificate - no password..."
openssl genrsa -out "$NGINX_CERTS_DIR/server-key.pem" 4096
openssl req -new -key "$NGINX_CERTS_DIR/server-key.pem" \
  -out "$NGINX_CERTS_DIR/server.csr" \
  -subj "/C=US/ST=State/L=City/O=Organization/CN=localhost"
openssl x509 -req -days 365 -in "$NGINX_CERTS_DIR/server.csr" \
  -CA "$RESOURCES_DIR/ca-cert.pem" -CAkey "$RESOURCES_DIR/ca-key.pem" \
  -CAcreateserial -out "$NGINX_CERTS_DIR/server-cert.pem"
chmod 600 "$NGINX_CERTS_DIR/server-key.pem"
echo "✓ Server certificate generated (no password)"
echo ""

# Copy CA cert to nginx certs directory (nginx needs it for client verification)
cp "$RESOURCES_DIR/ca-cert.pem" "$NGINX_CERTS_DIR/ca-cert.pem"
echo "✓ CA certificate copied to nginx-server/certs/"
echo ""

# Step 3: Generate Client Certificate (with password) - stored in resources
echo "Step 3: Generating Client Certificate - password protected..."
openssl genrsa -aes256 -passout pass:${CLIENT_PASS} -out "$RESOURCES_DIR/client-key.pem" 4096
openssl req -new -key "$RESOURCES_DIR/client-key.pem" -passin pass:${CLIENT_PASS} \
  -out "$RESOURCES_DIR/client.csr" \
  -subj "/C=US/ST=State/L=City/O=Organization/CN=client"
openssl x509 -req -days 365 -in "$RESOURCES_DIR/client.csr" \
  -CA "$RESOURCES_DIR/ca-cert.pem" -CAkey "$RESOURCES_DIR/ca-key.pem" \
  -CAcreateserial -out "$RESOURCES_DIR/client-cert.pem"
echo "✓ Client certificate generated (password: ${CLIENT_PASS})"
echo ""

echo "=== Certificate Generation Complete! ==="
echo ""
echo "Summary:"
echo "  CA & Client certs (PEM): $RESOURCES_DIR"
echo "    - ca-cert.pem, ca-key.pem (no password)"
echo "    - client-cert.pem, client-key.pem (password: ${CLIENT_PASS})"
echo ""
echo "  Server certs (PEM): $NGINX_CERTS_DIR"
echo "    - ca-cert.pem (copy)"
echo "    - server-cert.pem, server-key.pem (no password)"
echo ""
echo "Verify certificates:"
echo "  openssl verify -CAfile $RESOURCES_DIR/ca-cert.pem $NGINX_CERTS_DIR/server-cert.pem"
echo "  openssl verify -CAfile $RESOURCES_DIR/ca-cert.pem $RESOURCES_DIR/client-cert.pem"