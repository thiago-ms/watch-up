GRADLE_VERSION := 8.10.2
DC := docker compose
USERFLAG := --user $(shell id -u):$(shell id -g)
RUN := $(DC) run --rm $(USERFLAG) android

.PHONY: help image wrapper apk dist test clean shell

help:
	@echo "Alvos disponíveis:"
	@echo "  make image    - constrói a imagem Docker de build (JDK + Android SDK + Gradle)"
	@echo "  make wrapper  - gera o Gradle wrapper (necessário na 1ª vez)"
	@echo "  make apk      - gera o APK debug em app/build/outputs/apk/debug/"
	@echo "  make dist     - gera o APK com a versão no nome em dist/ (p/ distribuição)"
	@echo "  make test     - roda os testes unitários de todos os módulos"
	@echo "  make shell    - abre um shell dentro do container de build"
	@echo "  make clean    - limpa artefatos de build do Gradle"

image:
	$(DC) build

# Gera o wrapper apenas se ainda não existir (arquivo gradlew no host).
wrapper: image gradlew

gradlew:
	$(RUN) gradle wrapper --gradle-version $(GRADLE_VERSION)

apk: wrapper
	$(RUN) ./gradlew --no-daemon assembleDebug
	@echo ">> APK gerado em: app/build/outputs/apk/debug/app-debug.apk"

dist: wrapper
	$(RUN) ./gradlew --no-daemon :app:distApk

test: wrapper
	$(RUN) ./gradlew --no-daemon testDebugUnitTest

shell: image
	$(RUN) bash

clean: wrapper
	$(RUN) ./gradlew --no-daemon clean
