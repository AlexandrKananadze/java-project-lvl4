build:
	./gradlew clean build

install:
	./gradlew clean install

run-dist:
	./build/install/app/bin/app

lint:
	./gradlew checkstyleMain checkstyleTest

start:
	APP_ENV=development ./gradlew run

generate-migrations:
	./gradlew generateMigrations

check-updates:
	./gradlew dependencyUpdates

reports:
	./gradlew test

report:
	./gradlew jacocoTestReport
