# Plagiat_detection

## Installation

First of all it is necessary to prepare a PostgreSQL database where you will store the data.

```sql
create extension pg_trgm;
create table projekty (
    login varchar(7),
    file_name varchar(100) primary key ,
    doc text
);
create table podobne_projekty (
    query_file_name varchar(100),
    file_name varchar(100),
    sim float
);
```

Then download and compile this project. Requires Java (1.8+) and Maven.

```sh
git clone https://github.com/RadimBaca/Plagiat_detection
cd Plagiat_detection
mvn compile
```

## Run

The application parameter format is the following:

```
App [-i|-q] dir user password
```

- -i is an insert mode
- -q query mode
- dir the directory where the application finds the files
- user PostgreSQL user
- password PostgreSQL usr password
