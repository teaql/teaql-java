.PHONY: build run test
build:
	mvn -f ../java-lib-core/lib/pom.xml install -DskipTests
	mvn package -DskipTests
run: build
	mvn spring-boot:run
test:
	node ../spring-transactions/verify-local.cjs
