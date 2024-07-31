#!/bin/bash

kind create cluster --image kindest/node:v1.27.3 --name kind --config cluster-config.yaml