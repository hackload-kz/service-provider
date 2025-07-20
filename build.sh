#!/bin/bash

./mvnw clean install --file domain/pom.xml
./mvnw clean install --file application/pom.xml
./mvnw clean install --file infrastructure/pom.xml
