.PHONY: build run test

build:
	mvn -f ../java-lib-core/lib/pom.xml install -DskipTests
	mvn package -DskipTests

run: build
	mvn exec:java

test:
	mvn test
