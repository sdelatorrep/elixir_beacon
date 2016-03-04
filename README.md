#Requirements
* Java 8 JRE (or SDK if the code needs to be compiled)
* Apache Maven 3
* PostgreSQL Server or any other SQL server (i. e. MySQL)
* JMeter

#Configure databases
##Create databases
* Create two databases with the name you want (default names used by the application are **elixir_beacon_dev** and **elixir_beacon_testing**):
```
createdb database_name -h server_host -p server_port -U super_user
```
* Create a user that will be used by the application to connect to the databases just created:
```
createuser user_name
```
* Log in each of the databases and grant privileges to a normal user (that is, not a super user), i. e. the user just created in the previous step:
```
psql database_name -U super_user
```
```sql
GRANT ALL PRIVILEGES ON DATABASE database_name TO normal_user;
```
You can skip this step and load the schema using a super user in the next step and after that, granting privileges to a normal user (this user will be used by the application to connect to the database).

* Download the schema script from [here](https://raw.githubusercontent.com/sdelatorrep/elixir_beacon/master/src/main/resources/META-INF/elixir_beacon_db_schema.sql) and run it in **both** databases: 
```
psql -h server_host -p server_port -d database_name -U user_name < elixir_beacon_db_schema.sql
```
That script will create the schema and also load some essential data for data use conditions.

If you use a super user to create the schema then you will need to grant access to the "normal" user that will be used by the application (that user we created in the second step):
```sql
GRAN ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO USER user_name;
GRAN ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO USER user_name;
```
Remember to run these lines in both databases.

##Load the data
* Download the script to parse VCF files [here](https://raw.githubusercontent.com/sdelatorrep/elixir_beacon/master/src/main/resources/META-INF/vcf_parser.sh) and give it executable rights:
```
chmod +x vcf_parser.sh
```
* Run this script executing:
```
./vcf_parser.sh dataset_id < file.vcf
```
This script will generate an output file called dataset_id.SNPs.
It will also output the number of variants extracted from the VCF. This value is the "size" in the next step.

* Load the dataset information into **beacon_dataset_table**.
```sql
INSERT INTO beacon_dataset(id, description, access_type, reference_genome, size)
    VALUES ('dataset_id', 'dataset_description', 'i. e. PUBLIC', 'i. e. grch37', 123456);
```

* Load the generated file into **beacon_data_table**:
```
cat dataset_id.SNPs | psql -h server_host -p port -U user_name -c "COPY beacon_data_table(dataset_id,chromosome,position,alternate) FROM STDIN USING DELIMITERS ';' CSV" database_name
```

#Managing the code
##Download the project
Clone the projects **elixir_beacon** (current one) and **elixir_core** located at the [Elixir' repository](https://github.com/elixirhub/human-data-beacon).
```
git clone https://github.com/elixirhub/human-data-beacon.git
```
Then download each of the projects (submodules in git terminology):
```
cd human-data-beacon
cd elixir_beacon
git submodule init
git submodule update
```
Do the same for the other project.

##Structure
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

##Elixir Core
First of all, it is necessary to compile the code of the **elixir_core** project because it is a dependency of the main project, elixir_beacon.
```
mvn clean compile jar:jar
```
This will generate the JAR file **elixir-core-beacon_api_v0.3-SNAPSHOT.jar**

Then execute:
```
mvn install
```
Now this dependency will be found when compiling the main project, elixir_beacon.

##Configure application
The key file are **/src/main/resources/application-{profile}.properties** and **/src/test/resources/application-{profile}.properties** (see [Deploy JAR](https://github.com/sdelatorrep/elixir_beacon/blob/master/README.md#deploy-the-jar) for more information about profiles).

By default, the application is deployed at port **9075** and the context is **/elixirbeacon/v03/**. You can change this by modifying the following lines of the properties file:
```INI
server.port=9075
server.servlet-path=/v03
server.context-path=/elixirbeacon
```
By default, the application uses two PostgreSQL databases with the name **elixir_beacon_dev** and **elixir_beacon_testing** (the latter is used to run the tests).
```INI
datasource.elixirbeacon.url=jdbc:postgresql://hostname:port/elixir_beacon_dev
datasource.elixirbeacon.username=the_username
datasource.elixirbeacon.password=the_password
datasource.elixirbeacon.driverClassName=org.postgresql.Driver
```
1. Specify the type of the database (postgresql), the hostname, port and finally the database name.
    * I. e. if you use MySQL: jdbc:mysql
2. Username that will be used to connect to the database.
3. Password of that username.
4. Driver class name 
    * if you use MySQL: com.mysql.jdbc.Driver
```INI
spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.PostgreSQLDialect
```
1. Hibernate dialect.
    * if you use MySQL: org.hibernate.dialect.MySQLDialect
 
If you use a different DB than Postgres, you must add the corresponding library to the **/lib** folder inside the JAR (you don't need to recompile) or add the dependency to the pom.xml so maven can download the library (this will force you to compile).

##Compile and test the code
To compile the code run the following command within the project folder:
```
mvn clean compile package -Dspring.profiles.active="dev"
```
That will also execute the tests. To skip them add <code>-Dmaven.test.skip=true</code> to the command.

To execute only the tests run:
```
mvn test
```
NOTE: To execute the tests you should use a different database than the main one (i. e. elixir_beacon_testing, see [Create databases](https://github.com/sdelatorrep/elixir_beacon/blob/master/README.md#create-databases)).

If compilation and test execution are successful, a JAR file will be generated in the folder **/target** with the name **elixir-beacon-0.3.jar**

##Deploy the JAR
To deploy the JAR run run the following command within the /elixir_beacon/target folder:
 ```
java -jar elixir-beacon-0.3.jar --spring.profiles.active=dev
 ```
It will generate a log in the file **application.log** located in the same folder where the JAR is located.

This argument <code>--spring.profiles.active=dev</code> specifies the profile to be used. By default, there are 2 profiles: **dev** and **test**. Each profile will use its own set of properties files. 

I. e. **dev** profile will use:
* application-dev.properties
* application-dev.yml

Using the default configuration, the application will be available at: [localhost:9075/elixirbeacon/v03/](http://localhost:9075/elixirbeacon/v03/)

##Run integration tests
We use JMeter to run this kind of tests. We have an artifact called **elixir-beacon-service-tests**. 

To get the code download the project elixir_beacon_tests (you should already have the folder created but empty, follow [these steps](https://github.com/sdelatorrep/elixir_beacon/blob/master/README.md#download-the-project) to download the content).

Once you have downloaded this project you can just run:
```
mvn -P local clean verify
 ```
This will download jmeter and run some basic tests.

The <code>-P local</code> refers to a maven profile. These profiles can be found in the file pom.xml. The **local** profile uses this configuration for running the tests:
* host: localhost 
* port: 9075 

For other configurations please add a profile in pom.xml file. You will see the results on the console.

#Using the application
The application publishes two endpoints:
* /beacon/info
* /beacon/query

They are defined in the **org.ega_archive.elixirbeacon.ElixirBeaconController** class.

##/beacon/info
Returns the information about this beacon: its Id, name and description, the API version it is compliant with, the URL where you can access this beacon, etc.

[localhost:9075/elixirbeacon/v03/beacon/info](http://localhost:9075/elixirbeacon/v03/beacon/info)
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
The 3 examples that appear in field sampleAlleleRequests can be customized by modifying **application-{profile}.yml** as explained in [Configure application](https://github.com/sdelatorrep/elixir_beacon/blob/master/README.md#configure-application).

##/beacon/query
To actually ask the beacon for questions like "do you have any genomes with an 'A' at position 100,735 on chromosome 3?" And the answer will be yes or no.

Parameters:
* assemblyId
    * I. e. GRCh37
* referenceName
    * Accepted values: 1-22, X, Y, MT
* position
* alternate (optional)
    * Regular expression: [ACTG]+
    * I. e. alternate=A, alternate=CG, etc.
* datasetIds (optional)
    * I. e. ?datasetIds=some-id&datasetIds=another-id

[localhost:9075/elixirbeacon/v03/beacon/query?referenceName=1&position=179832996&assemblyId=GRCh37](http://localhost:9075/elixirbeacon/v03/beacon/query?referenceName=1&position=179832996&assemblyId=GRCh37)
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

[localhost:9075/elixirbeacon/v03/beacon/query?referenceName=1&position=179832996&assemblyId=GRCh37&datasetIds=EGAD00001000740](http://localhost:9075/elixirbeacon/v03/beacon/query?referenceName=1&position=179832996&assemblyId=GRCh37&datasetIds=EGAD00001000740)
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

#Extend/Change functionality
You have two options:

1. Editing the source code.
    * If you want to add new functionalities (i. e. new endpoints).
2. Changing the implementation class.
    * If you want to change the way something is done (i. e. you want to modify the query, to check some requirements in the parameters, etc.)
    * You can write your own implementation for the interface **org.ega_archive.elixirbeacon.ElixirBeaconService**
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
        * After That create the package **org.ega_archive.custom.elixirbeacon.** (by default, our app will scan org.ega_archive.custom.elixirbeacon package to try to find candidates for our services) If you want to use a different package name, you must cusomize your application properties name and add the property:
        ```INI
        custom.package.scan=org.my.custom
        ```
        * Inside that folder create a services package and a write your custom implementation 
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
        ```
        mvn clean compile jar:jar
        ```
        This will generate a **elixir-beacon-custom-version.jar**.
        
        If you get an error of the depency not found, it it because you don't have a repo with the dependency artifact. In this case, you can go to the elixir-beacon aritfact and execute:
        ```
        mvn install
        ```
        This will install the artifact in your local repo. After that try to compile again your custom code.
        * Execute the program with your code: 
            * First create an empty folder an copy there the original elixir jar (elixir-beacon-0.3.jar)
            * Then create a /lib folder and put the elixir-beacon-custom-version.jar jar in that folder
            * After that you can run the program executing:
            ```
            java -Dloader.path=lib/ -Dspring.profiles.active=dev -jar elixir-beacon-0.3.jar
            ```
