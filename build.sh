#!/bin/sh

set -ex

./sbt --sbt-jar sbt-launch.jar "show assembly"
