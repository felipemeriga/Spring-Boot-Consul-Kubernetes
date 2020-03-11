#!/bin/bash

/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

brew tap weaveworks/tap

brew install weaveworks/tap/eksctl

eksctl create cluster \
--name prod \
--version 1.14 \
--region us-east-2