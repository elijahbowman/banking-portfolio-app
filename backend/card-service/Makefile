# build-CardServiceFunction:
# 	mvn clean package -DskipTests -Dspring.profiles.active=local
#
# 	cp target/*.jar $(ARTIFACTS_DIR)
#
# .PHONY: build-CardServiceFunction

compile-project:
	mvn clean package -U -DskipTests -Ddependency.check.skip=true

build-CardServiceAPIFunction: compile-project
	mkdir -p $(ARTIFACTS_DIR)/lib
	cp target/card-service-lambda.jar $(ARTIFACTS_DIR)/lib/

build-CardServiceAccountCreatedListenerFunction: compile-project
	mkdir -p $(ARTIFACTS_DIR)/lib
	cp target/card-service-lambda.jar $(ARTIFACTS_DIR)/lib/

.PHONY: compile-all build-CardServiceAPIFunction build-CardServiceAccountCreatedListenerFunction