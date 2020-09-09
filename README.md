# aerospike-migration

A CLI application to migrate aerospike data from postgresql to aerospike

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
```

## EDN File

The migration is driven via the edn file provided with the option `edn-filepath`. `sample-mapping.edn` gives an example of 
migrating tables `users` and `movies` from postgres into sets, `user` and `movie` in aerospike. The structure of the edn file is as follows:

- The top level keys are names of relations in `kebab-case` (They are converted into snake case in the query).
- The values corresponding to the top level keys describe the columns, column to bin mapping, primary-keys, primary-keys to index mapping, 
batch size, row-count and transformer functions.
- You will notice that most of the keys are `namespaced` keywords referring to the specs in `aerospike-migration.spec` namespace.

Let's walk through the important keys in edn:

- `aerospike-migration.spec/columns` describes all the columns that will be used in the generated `SELECT` query. 
- `aerospike-migration.spec/bin` is the bin name corresponding to the column
- `aerospike-migration.spec/function` is the transformer function that is applied to the column before it goes into a bin or an index. The function can only 
be one of the functions defined in the namespace `aerospike-migration.transformer`.
- `aerospike-migration.transformer/primary-keys` is a vector of the keys that will be used in the index in aerospike. These should be part of `aerospike-migration.transformer/columns`.
- `aerospike-migration.transformer/prepend` and `aerospike-migration.transformer/append` are the value that will be prepended and appended to the index, respectively.
- `aerospike-migration.transformer/delimiter` is the delimiter used when the index is derived using more than one primary key. The primary key values will be joined using the delimiter and then prepended an appended with the `aerospike-migration.transformer/prepend` and `aerospike-migration.transformer/append` values.
- Just like columns every primary key value can also be made to go through a transformer before it is used as an index.
- `aerospike-migration.spec/batch-size` is the number of rows that will be concurrently sent to aerospike. Increasing this number might starv the number of connections to aerospike.
- `aerospike-migration.spec/row-count` is an optional key which, if provided, will be used to lazily generate a sequence (each of size `aerospike-migration.spec/batch-size`).

## GRAALVM

This program is graalvm compatible. To generate the native image,

1. `export NATIVE_IMAGE=<path-to>/graalvm-ce-java8-20.2.0/Contents/Home/bin`
2. Run the native image generator script included in the repo - `./generate-native-image.sh`

The 2nd command will generate a image with the name `aerospike-migration`. You can run it with,

```
 ./aerospike-migration  --hosts "localhost" --namespace "test" --db-host "127.0.0.1" \
 --db-port 5432 --db-name "test" --db-user "postgres" --db-pass "" \
 migrate --edn-filepath "/some-path/sample-mapping.edn" \
```

## Note

- `json` and `jsonb` columns are converted to json before inserting into aerospike and go into aerospike as `MAP`.
- Failures are logged into a file named `<relation-name>-debug-logger.txt` in the same directory as the jar.
