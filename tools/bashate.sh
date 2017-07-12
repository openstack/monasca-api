#!/usr/bin/env bash

# Ignore too long lines error E006 from bashate and treat
# E005, E042 as errors.
SH_FILES=$(find ./devstack -type d -name files -prune -o -type f -name '*.sh' -print)
bashate -v -iE006 -eE005,E042 ${SH_FILES:-''}
