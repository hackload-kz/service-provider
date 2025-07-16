@echo off

call mvnw.cmd clean install --file domain/pom.xml
call mvnw.cmd clean install --file application/pom.xml
call mvnw.cmd clean install --file infrastructure/pom.xml
