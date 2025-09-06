#!/usr/bin/env bash
set -euo pipefail
if [ -x ./mvnw ]; then
  ./mvnw -q -DskipTests package
  ./mvnw spring-boot:run
else
  mvn -q -DskipTests package
  mvn spring-boot:run
fi