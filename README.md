# aerospike-migration

A CLI application to migrate aerospike data from postgres to aero-spike

## Build

Build and create the jar with

```
make create-jar
```

A jar with name `aerospike-migration.jar` is created. Checkout the command line options with,

```
java -jar aerospike-migration.jar --help
```
## Usage

```
 java -jar aerospike-migration.jar  --hosts "localhost" --namespace "test" --db-host "127.0.0.1" \
 --db-port 5432 --db-name "test" --db-user "postgres" --db-pass "" \
 migrate --edn-filepath "/some-path/sample-mapping.edn" \
 --batch-size 100
```
