SHELL := /usr/bin/env bash -e

create-jar:
	lein uberjar
	cp target/segmentation-migration-0.1.0-standalone.jar .
	mv segmentation-migration-0.1.0-standalone.jar segmentation-migration.jar
