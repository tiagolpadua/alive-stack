#!/bin/sh

# http://localhost:9000/hello
# http://localhost:9000/q/health/live

./mvnw compile quarkus:dev -Dquarkus.http.port=9002
