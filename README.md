# Plagiat_detection

## Installation

First of all it is necessary to prepare a PostgreSQL database where you will store the data. We use files having a format where the first six or seven characters is a person login. Therefore we automaticaly extract the login from the file name and store it in the projekty table.

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

Then download and compile this project. Requires Java (1.8+) and Maven. Small changes need to be done in the code before you compile. You need to setup the host name in the App.java before compile in order to make it work.

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

- -i is an insert mode, this option will parse the files in the input directory (dir) and store it in the projekty table.
- -q query mode, this option will parse the files in the dir, finds the ten most similar documents stored in the database and store them in the podobne_projekty table.
- dir the directory where the application finds the files
- user PostgreSQL user
- password PostgreSQL usr password

For example we can run the jar file create during the compilation like this

```sh
java -cp target\PdfAnalyzer-1.0-SNAPSHOT.jar cz.vsb.baca.App -i insert_dir username password
```

Use the following query to see the most similar documents.

```
select *
from podobne_projekty
order by sim desc
```
