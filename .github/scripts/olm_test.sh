#!/bin/bash
set -e

TAG=${TAG:-latest}
export TEMPLATES=${PWD}/templates/build/enmasse-${TAG}

echo "Running OLM tests"
time make SYSTEMTEST_ARGS=olm.** systemtests
