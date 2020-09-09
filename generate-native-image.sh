#!/usr/bin/env bash

if [ -z "$NATIVE_IMAGE" ]; then
    echo 'Please set $NATIVE_IMAGE'
    exit 1
fi

$NATIVE_IMAGE --report-unsupported-elements-at-runtime  -jar ~/source_code/aerospike/aerospike-migration/aerospike-migration.jar \
  -H:Name=aerospike-migration --no-fallback  -H:+ReportExceptionStackTraces --initialize-at-build-time --no-fallback --no-server \
 --initialize-at-run-time=org.postgresql.sspi.SSPIClient
