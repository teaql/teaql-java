.PHONY: build run test
build:
	mvn install -DskipTests
run: build
	mvn -f java-app-console/pom.xml spring-boot:run
test:
	bash verify.sh
