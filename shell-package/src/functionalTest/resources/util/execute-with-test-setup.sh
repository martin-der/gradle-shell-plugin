#!/bin/bash

set -ue

SCRIPT_PATH=$(cd "$(dirname "$0")" && pwd -P)/$(basename "$0")

EXECUTABLE=$1

shift

export PATH="$(dirname "${SCRIPT_PATH}")/bin:${PATH}"

${EXECUTABLE} "${@}"
