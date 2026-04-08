compile-project:
	mvn clean package -U -DskipTests -Ddependency.check.skip=true

build-CardServiceApiFunction: compile-project
	mkdir -p $(ARTIFACTS_DIR)/lib
	cp target/card-service-lambda.jar $(ARTIFACTS_DIR)/lib/

build-CardServiceAccountCreatedListenerFunction: compile-project
	mkdir -p $(ARTIFACTS_DIR)/lib
	cp target/card-service-lambda.jar $(ARTIFACTS_DIR)/lib/

.PHONY: compile-all build-CardServiceApiFunction build-CardServiceAccountCreatedListenerFunction