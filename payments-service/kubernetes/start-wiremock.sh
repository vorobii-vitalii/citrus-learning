#!/bin/bash

# shellcheck disable=SC2164
cd /work/wiremock/helm-charts
helm upgrade --install wiremock ./charts/wiremock
