SHELL := /usr/bin/env bash -e

create-jar:
	lein uberjar
	cp target/aerospike-migration-0.1.0-standalone.jar .
	mv aerospike-migration-0.1.0-standalone.jar aerospike-migration.jar
