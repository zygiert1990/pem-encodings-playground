#!/bin/bash

set -e

# Define password for client key
CLIENT_PASS="MyClientPassword123"

# Define keystore passwords
KEYSTORE_PASS="keystorePassword"
TRUSTSTORE_PASS="truststorePassword"

# Define directories (relative to script location)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
RESOURCES_DIR="$PROJECT_ROOT/src/main/resources"

echo "=== Generating PKCS12 Keystores ==="
echo ""
echo "Resources dir: $RESOURCES_DIR"
echo ""

# Step 1: Create Keystore (client certificate and private key)
echo "Step 1: Creating client keystore..."

openssl pkcs12 -export \
  -in "$RESOURCES_DIR/client-cert.pem" \
  -inkey "$RESOURCES_DIR/client-key.pem" \
  -passin pass:${CLIENT_PASS} \
  -out "$RESOURCES_DIR/client-keystore.p12" \
  -name client \
  -passout pass:${KEYSTORE_PASS}

echo "✓ Client keystore generated: client-keystore.p12 (password: ${KEYSTORE_PASS})"
echo ""

# Step 2: Create Truststore (CA certificate)
echo "Step 2: Creating truststore..."

# Convert CA certificate to PKCS12 format
keytool -import \
  -file "$RESOURCES_DIR/ca-cert.pem" \
  -alias ca \
  -keystore "$RESOURCES_DIR/client-truststore.p12" \
  -storetype PKCS12 \
  -storepass ${TRUSTSTORE_PASS} \
  -noprompt

echo "✓ Truststore generated: client-truststore.p12 (password: ${TRUSTSTORE_PASS})"
echo ""

echo "=== PKCS12 Generation Complete! ==="
echo ""
echo "Generated files in $RESOURCES_DIR:"
echo "  - client-keystore.p12 (password: ${KEYSTORE_PASS})"
echo "  - client-truststore.p12 (password: ${TRUSTSTORE_PASS})"
echo ""