#Requirements
Java 8 JRE (or SDK if the code needs to be compiled)
Apache Maven 3
PostgreSQL Server or any other SQL server (i. e. MySQL)
JMeter

#Configure databases
##Create databases
Create a DB with the name you want (default name is elixir_beacon_dev):
```
createdb database_name -h server_host -p server_port -U super_user
```
Log in the DB and grant privileges to a normal user (that is, not a super user):
```
psql database_name -U super_user
GRANT ALL PRIVILEGES ON DATABASE database_name TO normal_user;
```
Load the schema dump into the DB (download it here):
```
psql -h server_host -p server_port -d database_name -U user_name < schema_dump.sql
```
You can skip step 2) and load the schema using a super user in step 3) and after that granting privileges to a normal user (this user will be used by the application to connect to the database).
Create a second database (i. e. elixir_beacon_testing) that will be used in the tests.

##Load the data
Use this script to parse a VCF input file:
```
#!/bin/bash
grep -v "#"| cut -f1,2,4,5,7 | sort | uniq | awk -v ds=$1 '
{ if ( ($5 == "PASS" || $5 == "InDel" || $5 == ".") )
    if ( numTokens=split($4,tokens,",") )
        for( i = 1; i <= numTokens; i ++ ) {
            print ds";"$1";"$2";"$3";"tokens[i]
        }
    }
' > file.SNPs
```
Run this script executing:
```
./vcf_parser.sh dataset_id < file.vcf
```
This script will generate an output file called file.SNPs.
Load the dataset information into beacon_dataset table.
```
INSERT INTO beacon_dataset(id, description, access_type, reference_genome, size)
    VALUES ('dataset_id', 'dataset_description', 'i. e. PUBLIC', 'i. e. grch37', 123456);
```
Load the generated file into beacon_data table:
```
cat file.SNPs | psql -h server_host -p port -U user_name -c "COPY table_name(dataset_id,chromosome,position,reference,alternate) FROM STDIN USING DELIMITERS ';' CSV" database_name
```

#Managing the code
##Download the project
Execute a git pull from this repository.
The project has the following structure:
* /src/main/java
    *Java files (.java).
* /src/main/resources
    * configuration files: .properies, .yml
* /src/test/java
    * Java classes for testing.
* /src/test/resources
    * configuration files for testing: .properties, .yml
* /target/generated-sources/java
    * auto generated Java files.
* /target/classes
    * compiled files (.class).
* /target
    * among other things, contains the .jar file with the compiled classes, libraries, etc.

Configure application
The key file is: /src/main/resources/application-{profile}.properties (see below Deploy JAR for more information about profiles).
By default, the application is deployed at port 9075 and the context is elixirbeacon/v03/. You can change this by modifying the following lines of the properties file:
application-dev.properties
server.port=9075
server.servlet-path=/v03
server.context-path=/elixirbeacon
By default, the application uses two PostgreSQL databases with the name elixir_beacon_dev and elixir_beacon_testing (the latter is used to run the tests).
```
datasource.elixirbeacon.url=jdbc:postgresql://hostname:port/elixir_beacon_dev
datasource.elixirbeacon.username=the_username
datasource.elixirbeacon.password=the_password
datasource.elixirbeacon.driverClassName=org.postgresql.Driver
spring.jpa.hibernate.naming-strategy = org.hibernate.cfg.ImprovedNamingStrategy
spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.connection.charSet = UTF-8
```
1. specify the type of the database (postgresql), the hostname, port and finally the database name.
    * I. e. if you use MySQL: jdbc:mysql
2. username that will be used to connect to the database.
3. password of that username.
4. driver class name 
    * if you use MySQL: com.mysql.jdbc.Driver
5. Nothing to change.
6. hibernate dialect.
    * if you use MySQL: org.hibernate.dialect.MySQLDialect
7. Nothing to change.
 

Do the same changes in /src/test/resources/application-{profile}.properties. By default, the database there is called elixir_beacon_testing.
If you use a different DB than Postgres, you must add the corresponding library to the /lib folder inside the JAR or add the dependency to the pom.xml so maven can download the library (this will allow you to compile the code if you haven't done this yet).
You can also change the sample requests that appear in the /info endpoint (see below Using the application) by modifying the values in application-{profile}.yml file:
```yml
#sample #1
querySamples:
  reference-set-1: GRCh37
  position-1: 6689
  chromosome-1: 17
  alternate-bases-1:
  dataset-ids-1:
#sample #2
  reference-set-2: GRCh37
  position-2: 1040026
  chromosome-2: 1
  alternate-bases-2:
  dataset-ids-2: EGAD00001000740,EGAD00001000741
#sample #3
  reference-set-3: GRCh37
  position-3: 1040026
  chromosome-3: 1
  alternate-bases-3: C
  dataset-ids-3: EGAD00001000740
```
##Compile and test the code
To compile the code run:
```
mvn clean compile package -Dspring.profiles.active="dev"
```
That will also execute the tests. To skip them add -Dmaven.test.skip=true to the command.
To execute only the tests run:
```
mvn test
```
NOTE: To execute the tests you should have a database different than the main one (see the previous section).
If compilation and test execution is successful, a JAR file will be generated in the folder /target with the name elixir-beacon-0.3.jar

