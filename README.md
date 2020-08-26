# segmentation-migration

A CLI application to migrate segmentation data from postgres to aero-spike

## Usage

Build and create the jar with

```
make create-jar
```

A jar with name `segmentation-migration.jar` is created. Checkout the command line options with

```
java -jar segmentation-migration.jar --help
```

Example, migrate the segments table to aerospike using a csv

```
java -jar segmentation-migration.jar --hosts "localhost" --namespace "test" ms \
 --filepath "/Users/foo/segmentation-migration/segments-1.csv" --batch-size 10
```
