#Requirements
* Java 8 JRE (or SDK if the code needs to be compiled)
* Apache Maven 3
* PostgreSQL Server or any other SQL server (i. e. MySQL)
* JMeter

#Configure databases
##Create databases
Create a DB with the name you want (default name is elixir_beacon_dev):
```bash
createdb database_name -h server_host -p server_port -U super_user
```
Log in the DB and grant privileges to a normal user (that is, not a super user):
```bash
psql database_name -U super_user
```
```sql
GRANT ALL PRIVILEGES ON DATABASE database_name TO normal_user;
```
Load the schema dump into the DB (download it here):
```bash
psql -h server_host -p server_port -d database_name -U user_name < schema_dump.sql
```
You can skip step 2) and load the schema using a super user in step 3) and after that granting privileges to a normal user (this user will be used by the application to connect to the database).
Create a second database (i. e. elixir_beacon_testing) that will be used in the tests.

##Load the data
Use this script to parse a VCF input file:
```bash
#!/bin/bash
grep -v "#"| cut -f1,2,4,5,7 | sort | uniq | awk -v ds=$1 '
{ if ( (length($3) == 1 && length($4) == 1) && ($5 == "PASS" || $5 == ".")) print ds";"$1";"$2";"$4}
' > file.SNPs
```
Run this script executing:
```bash
./vcf_parser.sh dataset_id < file.vcf
```
This script will generate an output file called file.SNPs.
Load the dataset information into beacon_dataset table.
```sql
INSERT INTO beacon_dataset(id, description, access_type, reference_genome, size)
    VALUES ('dataset_id', 'dataset_description', 'i. e. PUBLIC', 'i. e. grch37', 123456);
```
Load the generated file into beacon_data table:
```bash
cat file.SNPs | psql -h server_host -p port -U user_name -c "COPY table_name(dataset_id,chromosome,position,alternate) FROM STDIN USING DELIMITERS ';' CSV" database_name
```

#Managing the code
##Download the project
Execute a git pull from this repository.

The project has the following structure:
* /src/main/java
    * Java files (.java).
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
```INI
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
```bash
mvn clean compile package -Dspring.profiles.active="dev"
```
That will also execute the tests. To skip them add -Dmaven.test.skip=true to the command.
To execute only the tests run:
```bash
mvn test
```
NOTE: To execute the tests you should have a database different than the main one (see the previous section).
If compilation and test execution is successful, a JAR file will be generated in the folder /target with the name elixir-beacon-0.3.jar

##Extend/Change functionality
You've got two options:

1. Editing the source code.
    * If you want to add new functionalities (i. e. new endpoints).
2. Changing the implementation class.
    * If you want to change the way something is done (i. e. you want to modify the query, to check some requirements in the parameters, etc.)
    * You can write your own implementation for the interface org.ega_archive.elixirbeacon.ElixirBeaconService
    * This application uses [Spring framework](http://docs.spring.io/spring/docs/4.0.x/spring-framework-reference/htmlsingle/). Specifically we use [Spring boot](https://docs.spring.io/spring-boot/docs/1.1.x/reference/htmlsingle/).
    * The following steps will allow you to make a custom implementation:
        * Create a new maven project:
        ```xml
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
          
            <groupId>org.ega_archive</groupId>
            <artifactId>elixir-beacon-custom</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <packaging>jar</packaging>
          
            <name>elixir-beacon-custom</name>
            <description>elixir-beacon-custom</description>
          
            <dependencies>
                <dependency>
                    <groupId>org.ega_archive</groupId>
                    <artifactId>elixir-beacon</artifactId>
                    <version>put version here, i.e: 0.0.1-SNAPSHOT</version>
                </dependency>
            </dependencies>
        </project>
        ```
        * After That create the package org.ega_archive.custom.elixirbeacon. (by default, our app will scan org.ega_archive.custom.elixirbeacon package to try to find candidates for our services) If you want to use a different package name, you must cusomizt your application properties name and add the property:
        ```
        custom.package.scan=org.my.custom
        ```
        * Inside that folder create a services package and a write your CustomImplementation 
        ```java
        package org.ega_archive.elixirbeacon.service;
        
        import org.ega_archive.elixirbeacon.dto.Beacon;
        import org.ega_archive.elixirbeacon.dto.BeaconAlleleResponse;
        import org.ega_archive.elixircore.helper.CommonQuery;
        import org.springframework.context.annotation.Primary;
        import org.springframework.stereotype.Component;
         
        import java.util.List;
         
        @Primary //This will make that this implementation will be used instead of the default one
        @Component
        public class CustomService implements ElixirBeaconService {
         
          public Beacon listDatasets(CommonQuery commonQuery, String referenceGenome) {
            //Write here your custom code
            return null;
          }
         
          public BeaconAlleleResponse queryBeacon(List<String> datasetStableIds, String alternateBases, String referenceBases, String chromosome, Integer position, String referenceGenome) {
            //Write here your custom code
            return null;
          }
         
          public List<String> checkParams(BeaconAlleleResponse result, List<String> datasetStableIds, String alternateBases, String referenceBases, String chromosome, Integer position, String referenceGenome) {
            //Write here your custom code
            return null;
          }
        }
        ```
        * Compile your code:
        ```bash
        mvn clean compile jar:jar
        ```
        This will generate a elixir-beacon-custom-version.jar
        If you get an error of the depency not found, it it because you don't have a repo with the dependency artifact. In this case, you can go to the elixir-beacon aritfact and execute:
        ```bash
        mvn install
        ```
        This will install the artifact in your local repo. After that try to compile again your custom code.
        * Execute the program with your code: First create an empty folder an copy there the original elixir jar (elixir-beacon-0.3.jar) then create a lib folder and put the elixir-beacon-custom-version.jar jar in that folder. After that you can run the program executing:
        ```bash
        java -Dloader.path=lib/ -Dspring.profiles.active=test -jar elixir-beacon-0.3.jar
        ```

##Deploy the JAR
To deploy the JAR run:
 ```bash
java -jar elixir-beacon-0.3.jar --spring.profiles.active=test
 ```
It will generate a log in the file application.log located in the same folder where the JAR is.
This argument --spring.profiles.active=test specifies the profile to be used. By default, there are 2 profiles: dev and test. Each profile will use its own set of properties files. I. e. test profile will use application-test.properties and application-test.yml
Using the default configuration, the application will be available at: localhost:9075/elixirbeacon/v03/

##Run integration tests
We use JMeter to run this kind of tests. We have an artifact called elixir-beacon-service-tests. To get the code execute a git pull from the elixir_beacon_tests project at [Elixir's Human Data Beacon repository](https://github.com/elixirhub/human-data-beacon).
Once you've downloaded this project you can just run:
```bash
mvn -P local clean verify
 ```
This will download jmeter and run some basic tests.
The " -P local" refers to a maven profile. These profiles can be found in the file pom.xml. The "local" profile uses this configuration for running the tests:
* host: localhost 
* port: 9075 
For other configurations please add a profile in pom.xml file. You will see the results on the console.

#Using the application
The application publishes two endpoints:
* /info
* /query

They're defined in the org.ega_archive.elixirbeacon.ElixirBeaconController class.

##/info
Returns the information about this beacon: its Id, name and description, the API version it is compliant with, the URL where you can access this beacon, etc.

https://egatest.crg.eu/elixir_demo_beacon/info
```json
{
  "id" : "elixir-demo-beacon",
  "name" : "Elixir Demo Beacon",
  "apiVersion" : "0.3",
  "organization" : {
    "id" : "EGA",
    "name" : "European Genome-Phenome Archive (EGA)",
    "description" : "The European Genome-phenome Archive (EGA) is a service for permanent archiving and sharing of all types of personally identifiable genetic and phenotypic data resulting from biomedical research projects.",
    "address" : "",
    "welcomeUrl" : "https://ega-archive.org/",
    "contactUrl" : "mailto:beacon.ega@crg.eu",
    "logoUrl" : "https://ega-archive.org/images/logo.png",
    "info" : null
  },
  "description" : "This <a href=\"http://ga4gh.org/#/beacon\">Beacon</a> is based on the GA4GH Beacon <a href=\"https://github.com/ga4gh/schemas/blob/beacon/src/main/resources/avro/beacon.avdl\"></a>",
  "version" : "v03",
  "welcomeUrl" : "https://ega-archive.org/elixir_demo_beacon/",
  "alternativeUrl" : "https://ega-archive.org/elixir_demo_beacon_web/",
  "created" : 1433116800000,
  "updated" : null,
  "datasets" : [ {
    "id" : "EGAD00001000740",
    "name" : null,
    "description" : "Low-coverage whole genome sequencing; variant calling, genotype calling and phasing",
    "assemblyId" : "grch37",
    "dataUseConditions" : [ {
      "@class" : "org.ega_archive.elixirbeacon.dto.ConsentCodeDataUseProfile",
      "header" : {
        "name" : "Consent Code",
        "version" : "0.1",
        "furtherDetails" : "http://journals.plos.org/plosgenetics/article?id=10.1371/journal.pgen.1005772"
      },
      "profile" : {
        "primaryCategory" : {
          "code" : "GRU",
          "description" : "For health/medical/biomedical purposes, including the study of population origins or ancestry."
        },
        "secondaryCategories" : [ {
          "code" : "NMDS",
          "description" : "Use of the data includes methods development research (e.g., development of software or algorithms) ONLY within the bounds of other data use limitations.",
          "details" : "Statistical Methods"
        } ],
        "requirements" : [ {
          "code" : "NPU",
          "description" : "Use of the data is limited to not-for-profit organizations."
        }, {
          "code" : "MOR",
          "description" : "Requestor agrees not to publish results of studies until [date].",
          "details" : "X-Expired"
        }, {
          "code" : "US",
          "description" : "Use of data is limited to use by approved users."
        }, {
          "code" : "PS",
          "description" : "Use of data is limited to use within an approved project."
        } ]
      }
    }, {
      "@class" : "org.ega_archive.elixirbeacon.dto.AdamDataUseProfile",
      "header" : {
        "name" : "ADA-M",
        "version" : "0.2",
        "furtherDetails" : "http://p3g.org/sites/default/files/site/default/files/ADAM_introductiontext_21Jan2016.pdf"
      },
      "profile" : {
        "anyResearch" : "UNTRUE",
        "anyMethodsDevelopmentResearch" : "TRUE",
        "anyGeneticResearch" : "TRUE",
        "anyNonProfitPurpose" : "TRUE",
        "allowedNonProfitPurposes" : "Advance and understaing of genetics and genomics , including the treatment of disorders, and work on statistical mehods tha migh be applied to such research. ",
        "noOtherConditions" : "UNTRUE",
        "whichOtherConditions" : "Data can be used as controls, A copy of the DAA has to be distributed to all authorized personal .",
        "noAuthorizationObligations" : "UNTRUE",
        "whichAuthorizationObligations" : "An applicant having signed this Data Access Agreement , whose institution has co-signed this Data Acces Agreement , both of them having received acknowledge of its acceptance ",
        "noPublicationObligations" : "TRUE",
        "noTimelineObligations" : "UNTRUE",
        "whichTimelineObligations" : "Agree to update the list of authorized personnel to reflect any changes or departures in affiliated researches and personnel within 30 days of the change made. ",
        "noExpungingObligations" : "UNTRUE",
        "whichExpungingObligations" : "Data helds, will be destroyed , once it is no longer used for the aaproved research, unlesss obliged to retain the data for the archival purposes in conformity with instituitonal policies.",
        "noLinkingObligations" : "UNTRUE",
        "whichLinkingObligations" : "Acknowledge the published paper, version of data and the role of the consortium.",
        "noIPClaimObligations" : "UNTRUE",
        "whichIPClaimObligations" : "Data is protected by international copywright laws, Nothing in the agreement shall operate to transfer to the user institution  any property or intelectual rights. The user institution has the right to develop property based on comparisons with their own data , but not to make intelectual property claims on the data, nor use intelectual property protecion in ways that would prevent, or block access to, or useof, any element of the Data, or conclusions drawn from the Data.  If results arising from the User and User Institution  use of the data could provide could provide health solutions for the benefit of people in the development world , the user and the user institution agree to offer non- exlusive licenses to such results on a reasonable basis for the use for the use in low income and low -middle income countries (as defined by the world bank) to any party that request suvh a license solely for the use within these territories.",
        "noReportingObligations" : "UNTRUE",
        "whichReportingObligations" : "A report must be submited to the DAC if requested on the agreed completion of purpose."
      }
    } ],
    "created" : null,
    "updated" : null,
    "version" : null,
    "variantCount" : 43623891,
    "callCount" : null,
    "sampleCount" : null,
    "externalUrl" : null,
    "info" : {
      "accessType" : "PUBLIC",
      "authorized" : "true"
    }
  } ],
  "sampleAlleleRequests" : [ {
    "alternateBases" : "A",
    "referenceBases" : null,
    "referenceName" : "1",
    "position" : 179832996,
    "assemblyId" : "GRCh37",
    "datasetIds" : null
  } ],
  "info" : {
    "size" : "87247782"
  }
}
```
The 3 examples that appear in field sampleAlleleRequests can be customized by modifying application-{profile}.yml as explained in Configure application.

##/query
To actually ask the beacon for questions like "do you have any genomes with an 'A' at position 100,735 on chromosome 3?" And the answer will be yes or no.

https://egatest.crg.eu/elixir_demo_beacon/query?referenceName=1&position=179832996&assemblyId=GRCh37)
```json
{
  "beaconId" : "elixir-demo-beacon",
  "exists" : true,
  "error" : null,
  "alleleRequest" : {
    "alternateBases" : "A",
    "referenceBases" : null,
    "referenceName" : "1",
    "position" : 179832996,
    "assemblyId" : "GRCh37",
    "datasetIds" : null
  },
  "datasetAlleleResponses" : null
}
```
Or you can ask for the same information in an specific dataset:

https://egatest.crg.eu/elixir_demo_beacon/query?referenceName=1&position=179832996&assemblyId=GRCh37&datasetIds=EGAD00001000740
```json
{
  "beaconId" : "elixir-demo-beacon",
  "exists" : true,
  "error" : null,
  "alleleRequest" : {
    "alternateBases" : "A",
    "referenceBases" : null,
    "referenceName" : "1",
    "position" : 179832996,
    "assemblyId" : "GRCh37",
    "datasetIds" : [ "EGAD00001000740" ]
  },
  "datasetAlleleResponses" : [ {
    "datasetId" : "EGAD00001000740",
    "exists" : true,
    "error" : null,
    "frequency" : null,
    "variantCount" : null,
    "callCount" : null,
    "sampleCount" : null,
    "note" : "OK",
    "externalUrl" : null,
    "info" : null
  } ]
}
```
