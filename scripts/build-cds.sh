#!/usr/bin/env bash
# Builds an Application Class Data Sharing (AppCDS) archive for BondApplication.
# Run this once after each build; the resulting app-cds.jsa cuts ~50-80ms off startup.
#
# Usage:
#   ./scripts/build-cds.sh
#
# Then launch the app with:
#   java -XX:+UseZGC -Xms64m -Xmx128m \
#        -Xshare:on -XX:SharedArchiveFile=app-cds.jsa \
#        -cp target/OOP_Exercises-1.0-SNAPSHOT.jar \
#        com.fixedIncome.BondApplication

set -euo pipefail

JAR="target/OOP_Exercises-1.0-SNAPSHOT.jar"
MAIN="com.fixedIncome.BondApplication"

if [ ! -f "$JAR" ]; then
  echo "ERROR: $JAR not found. Run 'mvn package' first."
  exit 1
fi

echo "Step 1/2: Recording loaded classes..."
java -Xshare:off \
     -XX:DumpLoadedClassList=classes.lst \
     -cp "$JAR" "$MAIN" > /dev/null 2>&1 || true

echo "Step 2/2: Building CDS archive..."
java -Xshare:dump \
     -XX:SharedClassListFile=classes.lst \
     -XX:SharedArchiveFile=app-cds.jsa \
     -cp "$JAR"

echo "Done. app-cds.jsa created."
echo ""
echo "Run with:"
echo "  java -XX:+UseZGC -Xms64m -Xmx128m \\"
echo "       -Xshare:on -XX:SharedArchiveFile=app-cds.jsa \\"
echo "       -cp $JAR $MAIN"
