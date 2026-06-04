
#!/bin/bash

set -e

# Define directories (relative to script location)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
RESOURCES_DIR="$PROJECT_ROOT/src/main/resources"

echo "=== Removing Keystores ==="
echo ""
echo "Resources dir: $RESOURCES_DIR"
echo ""

# Remove JKS files if they exist
if [ -f "$RESOURCES_DIR/client-keystore.jks" ]; then
  rm "$RESOURCES_DIR/client-keystore.jks"
  echo "✓ Removed: client-keystore.jks"
else
  echo "- Not found: client-keystore.jks"
fi

if [ -f "$RESOURCES_DIR/client-truststore.jks" ]; then
  rm "$RESOURCES_DIR/client-truststore.jks"
  echo "✓ Removed: client-truststore.jks"
else
  echo "- Not found: client-truststore.jks"
fi

# Remove PKCS12 files if they exist
if [ -f "$RESOURCES_DIR/client-keystore.p12" ]; then
  rm "$RESOURCES_DIR/client-keystore.p12"
  echo "✓ Removed: client-keystore.p12"
else
  echo "- Not found: client-keystore.p12"
fi

if [ -f "$RESOURCES_DIR/client-truststore.p12" ]; then
  rm "$RESOURCES_DIR/client-truststore.p12"
  echo "✓ Removed: client-truststore.p12"
else
  echo "- Not found: client-truststore.p12"
fi

echo ""
echo "=== Keystore Cleanup Complete! ==="