#!/bin/bash

helm install fluent-bit fluent/fluent-bit -f values.yaml
#helm install fluent-bit fluent/fluent-bit