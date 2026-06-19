#!/bin/bash
cd "$(dirname "$0")"
OUT_DIR="$(pwd)/out"
javac -cp "$OUT_DIR;sqlite-jdbc-3.45.1.0.jar" -d out src/habittracker/db/DatabaseManager.java
echo "Exit code: $?"
ls -la out/habittracker/db/ 2>/dev/null || echo "No db classes yet"
