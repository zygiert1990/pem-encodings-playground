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

echo "=== Generating JKS Keystores ==="
echo ""
echo "Resources dir: $RESOURCES_DIR"
echo ""

# Step 1: Create Keystore (client certificate and private key)
echo "Step 1: Creating client keystore..."

# First create PKCS12 (intermediate format)
openssl pkcs12 -export \
  -in "$RESOURCES_DIR/client-cert.pem" \
  -inkey "$RESOURCES_DIR/client-key.pem" \
  -passin pass:${CLIENT_PASS} \
  -out "$RESOURCES_DIR/client-keystore.p12" \
  -name client \
  -passout pass:${KEYSTORE_PASS}

# Convert PKCS12 to JKS
keytool -importkeystore \
  -srckeystore "$RESOURCES_DIR/client-keystore.p12" \
  -srcstoretype PKCS12 \
  -srcstorepass ${KEYSTORE_PASS} \
  -destkeystore "$RESOURCES_DIR/client-keystore.jks" \
  -deststoretype JKS \
  -deststorepass ${KEYSTORE_PASS} \
  -noprompt

# Remove intermediate PKCS12 file
rm "$RESOURCES_DIR/client-keystore.p12"

echo "✓ Client keystore generated: client-keystore.jks (password: ${KEYSTORE_PASS})"
echo ""

# Step 2: Create Truststore (CA certificate)
echo "Step 2: Creating truststore..."

keytool -import \
  -file "$RESOURCES_DIR/ca-cert.pem" \
  -alias ca \
  -keystore "$RESOURCES_DIR/client-truststore.jks" \
  -storepass ${TRUSTSTORE_PASS} \
  -noprompt

echo "✓ Truststore generated: client-truststore.jks (password: ${TRUSTSTORE_PASS})"
echo ""

echo "=== JKS Generation Complete! ==="
echo ""
echo "Generated files in $RESOURCES_DIR:"
echo "  - client-keystore.jks (password: ${KEYSTORE_PASS})"
echo "  - client-truststore.jks (password: ${TRUSTSTORE_PASS})"
echo ""