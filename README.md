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

* Load the schema into **both** databases:
```sql
CREATE TABLE beacon_dataset_table (
	id character varying(50) NOT NULL PRIMARY KEY,
	description character varying(800),
	access_type character varying(10),
	reference_genome character varying(50),
	size integer
);

CREATE TABLE beacon_data_table
(
  id serial NOT NULL PRIMARY KEY,
  dataset_id character varying(50) NOT NULL REFERENCES beacon_dataset_table(id),
  chromosome character varying(2) NOT NULL,
  "position" integer NOT NULL,
  alternate character varying(100) NOT NULL,
  UNIQUE (dataset_id, chromosome, "position", alternate)
);

CREATE OR REPLACE VIEW beacon_dataset AS 
	SELECT bdat.id,
		bdat.description,
		bdat.access_type,
		bdat.reference_genome,
		bdat.size
	FROM beacon_dataset_table bdat
	WHERE (bdat.access_type::text = ANY (ARRAY['PUBLIC'::character varying::text, 'REGISTERED'::character varying::text, 'CONTROLLED'::character varying::text])) 
	AND bdat.size > 0 AND bdat.reference_genome::text <> ''::text;

CREATE OR REPLACE VIEW beacon_data AS 
	SELECT bd.dataset_id,
		bd.chromosome,
		bd."position",
		bd.alternate,
		ebdat.reference_genome
	FROM beacon_data_table bd
	INNER JOIN beacon_dataset ebdat ON bd.dataset_id::text = ebdat.id::text;

-----------------------------------
---------- CONSENT CODES ----------
-----------------------------------
CREATE TABLE consent_code_category_table (
	id serial PRIMARY KEY,
	name character varying(11)
);

INSERT INTO consent_code_category_table(name) VALUES ('PRIMARY');
INSERT INTO consent_code_category_table(name) VALUES ('SECONDARY');
INSERT INTO consent_code_category_table(name) VALUES ('REQUIREMENT');

CREATE TABLE consent_code_table (
	id serial PRIMARY KEY,
	name character varying(100) NOT NULL,
	abbr character varying(4) NOT NULL,
	description character varying(400) NOT NULL,
	category_id int NOT NULL REFERENCES consent_code_category_table(id)
);

INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('No restrictions', 'NRES', 'No restrictions on data use.', 1);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('General research use and clinical care', 'GRU', 'For health/medical/biomedical purposes, including the study of population origins or ancestry.', 1);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Health/medical/biomedical research and clinical care', 'HMB', 'Use of the data is limited to health/medical/biomedical purposes; does not include the study of population origins or ancestry.', 1);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Disease-specific research and clinical care', 'DS', 'Use of the data must be related to [disease].', 1);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Population origins/ancestry research', 'POA', 'Use of the data is limited to the study of population origins or ancestry.', 1);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Oher research-specific restrictions', 'RS', 'Use of the data is limited to studies of [research type] (e.g., pediatric research).', 2);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Research use only', 'RUO', 'Use of data is limited to research purposes (e.g., does not include its use in clinical care).', 2);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('No “general methods” research', 'NMDS', 'Use of the data includes methods development research (e.g., development of software or algorithms) ONLY within the bounds of other data use limitations.', 2);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Genetic studies only', 'GSO', 'Use of the data is limited to genetic studies only (i.e., no “phenotype-only” research).', 2);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Not-for-profit use only', 'NPU', 'Use of the data is limited to not-for-profit organizations.', 3);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Publication required', 'PUB', 'Requestor agrees to make results of studies using the data available to the larger scientific community.', 3);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Collaboration required', 'COL', 'Requestor must agree to collaboration with the primary study investigator(s).', 3);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Ethics approval required', 'IRB', 'Requestor must provide documentation of local IRB/REC approval.', 3);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Geographical restrictions', 'GS', 'Use of the data is limited to within [geographic region].', 3);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Publication moratorium/embargo', 'MOR', 'Requestor agrees not to publish results of studies until [date].', 3);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Time limits on use', 'TS', 'Use of data is approved for [x months].', 3);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('User-specific restrictions', 'US', 'Use of data is limited to use by approved users.', 3);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Project-specific restrictions', 'PS', 'Use of data is limited to use within an approved project.', 3);
INSERT INTO consent_code_table(name, abbr, description, category_id) VALUES ('Institution-specific restrictions', 'IS', 'Use of data is limited to use within an approved institution.', 3);

CREATE TABLE beacon_dataset_consent_code_table (
	dataset_id character varying(50) NOT NULL REFERENCES beacon_dataset_table(id),
	consent_code_id int NOT NULL REFERENCES consent_code_table(id),
	detail character varying(1000),
	PRIMARY KEY (dataset_id, consent_code_id)
);

-----------------------------------
-------------- ADA-M --------------
-----------------------------------

CREATE TABLE adam_value_table(
	id serial PRIMARY KEY,
	value character varying(13) NOT NULL
);

INSERT INTO adam_value_table(value) VALUES ('NOT SPECIFIED');
INSERT INTO adam_value_table(value) VALUES ('UNTRUE');
INSERT INTO adam_value_table(value) VALUES ('TRUE');

CREATE TABLE adam_table(
	id serial PRIMARY KEY,
	attribute character varying(50) NOT NULL CONSTRAINT adam_attribute_unique UNIQUE,
	description character varying(400)	
);

INSERT INTO adam_table(attribute, description) VALUES ('anyCountry','within any country/location');
INSERT INTO adam_table(attribute, description) VALUES ('allowedCountries','within specified countries/locations');
INSERT INTO adam_table(attribute, description) VALUES ('excludedCountries','within any country/location other than those specified');
INSERT INTO adam_table(attribute, description) VALUES ('anyOrganisation','by all organisations');
INSERT INTO adam_table(attribute, description) VALUES ('anyNonProfitOrganisation','by any non-profit organisations');
INSERT INTO adam_table(attribute, description) VALUES ('allowedNonProfitOrganisations','by specified non-profit organisations');
INSERT INTO adam_table(attribute, description) VALUES ('excludedNonProfitOrganisations','by any non-profit organisations other than those specified');
INSERT INTO adam_table(attribute, description) VALUES ('anyProfitOrganisation','by any profit organisations');
INSERT INTO adam_table(attribute, description) VALUES ('allowedProfitOrganisations','by specified profit organisations');
INSERT INTO adam_table(attribute, description) VALUES ('excludedProfitOrganisations','by any profit organisation other than those specified');
INSERT INTO adam_table(attribute, description) VALUES ('anyPerson','by any category of person');
INSERT INTO adam_table(attribute, description) VALUES ('anyAcademicProfessional','by any category of academic professional');
INSERT INTO adam_table(attribute, description) VALUES ('allowedAcademicProfessionals','by specified categories of academic professional');
INSERT INTO adam_table(attribute, description) VALUES ('excludedAcademicProfessionals','by any category of academic professional other than those specified');
INSERT INTO adam_table(attribute, description) VALUES ('anyClinicalProfessional','by any category of clinical professional');
INSERT INTO adam_table(attribute, description) VALUES ('allowedClinicalProfessionals','by specified categories of clinical professional');
INSERT INTO adam_table(attribute, description) VALUES ('excludedClinicalProfessionals','by any category of clinical professional other than those specified');
INSERT INTO adam_table(attribute, description) VALUES ('anyProfitmakingProfessional','by any category of profit-making professional');
INSERT INTO adam_table(attribute, description) VALUES ('allowedProfitmakingProfessionals','by specified categories of profit-making professional');
INSERT INTO adam_table(attribute, description) VALUES ('excludedProfitmakingProfessionals','by any category of profit-making professional other than those specified');
INSERT INTO adam_table(attribute, description) VALUES ('anyNonProfessional','by any category of non-professional');
INSERT INTO adam_table(attribute, description) VALUES ('allowedNonProfessionals','by specified categories of non-professional');
INSERT INTO adam_table(attribute, description) VALUES ('excludedNonProfessionals','by any category of non-professional other than those specified');
INSERT INTO adam_table(attribute, description) VALUES ('anyDomain','for any domain');
INSERT INTO adam_table(attribute, description) VALUES ('anyResearch','for any research purpose');
INSERT INTO adam_table(attribute, description) VALUES ('anyFundamentalBiologyResearch','for research w.r.t. fundamental biology');
INSERT INTO adam_table(attribute, description) VALUES ('anyMethodsDevelopmentResearch','for research w.r.t. methods development');
INSERT INTO adam_table(attribute, description) VALUES ('anyPopulationResearch','for research w.r.t. populations');
INSERT INTO adam_table(attribute, description) VALUES ('anyAncestryResearch','for research w.r.t. ancestry');
INSERT INTO adam_table(attribute, description) VALUES ('anyGeneticResearch','for research w.r.t. genetics');
INSERT INTO adam_table(attribute, description) VALUES ('anyDrugDevelopmentResearch','for research w.r.t. drug development');
INSERT INTO adam_table(attribute, description) VALUES ('anyDiseaseResearch','for research w.r.t. any disease');
INSERT INTO adam_table(attribute, description) VALUES ('allowedDiseasesResearch','for research w.r.t. any disease other than those specified');
INSERT INTO adam_table(attribute, description) VALUES ('excludedDiseasesResearch','for research w.r.t. specified diseases');
INSERT INTO adam_table(attribute, description) VALUES ('allowedAgeCategoriesResearch','for research w.r.t. specified age categories');
INSERT INTO adam_table(attribute, description) VALUES ('allowedGenderCategoriesResearch','for research w.r.t. specified gender categories');
INSERT INTO adam_table(attribute, description) VALUES ('allowedOtherResearch','for other specified categories of research');
INSERT INTO adam_table(attribute, description) VALUES ('anyClinicalCare','for any clinical care purpose');
INSERT INTO adam_table(attribute, description) VALUES ('anyDiseasesClinicalCare','for clinical care w.r.t.  any disease');
INSERT INTO adam_table(attribute, description) VALUES ('allowedDiseasesClinicalCare','for clinical care w.r.t.  any disease other than those specified');
INSERT INTO adam_table(attribute, description) VALUES ('excludedDiseasesClinicalCare','for clinical care w.r.t. specified diseases');
INSERT INTO adam_table(attribute, description) VALUES ('allowedOtherClinicalCare','for other specified categories of clinical care');
INSERT INTO adam_table(attribute, description) VALUES ('anyProfitPurpose','for any profit purpose');
INSERT INTO adam_table(attribute, description) VALUES ('allowedProfitPurposes','for specified profit purposes');
INSERT INTO adam_table(attribute, description) VALUES ('excludedProfitPurposes','for any profit purpose other than those specified');
INSERT INTO adam_table(attribute, description) VALUES ('anyNonProfitPurpose','for any non-profit purpose');
INSERT INTO adam_table(attribute, description) VALUES ('allowedNonProfitPurposes','for specified non-profit purposes');
INSERT INTO adam_table(attribute, description) VALUES ('excludedNonProfitPurposes','for any non-profit purpose other than those specified');
INSERT INTO adam_table(attribute, description) VALUES ('metaConditions','Meta-Conditions:');
INSERT INTO adam_table(attribute, description) VALUES ('noOtherConditions','There are no other restrictions/limitations in force which are not herein specified');
INSERT INTO adam_table(attribute, description) VALUES ('whichOtherConditions','Other permissions/limitations may apply as specified');
INSERT INTO adam_table(attribute, description) VALUES ('sensitivePopulations','No special evaluation required for access requests involving sensitive/restricted populations');
INSERT INTO adam_table(attribute, description) VALUES ('uniformConsent','Identical consent permissions have been provided by all subjects');
INSERT INTO adam_table(attribute, description) VALUES ('termsOfAgreement','Terms of agreement:');
INSERT INTO adam_table(attribute, description) VALUES ('noAuthorizationObligations','There are no requirements for any formal approval, contract or review conditions to be satisfied');
INSERT INTO adam_table(attribute, description) VALUES ('whichAuthorizationObligations','Formal approval, contract or review conditions are to be met, as specified');
INSERT INTO adam_table(attribute, description) VALUES ('noPublicationObligations','There are no requirements regarding publication or disclosure of derived results');
INSERT INTO adam_table(attribute, description) VALUES ('whichPublicationObligations','Publication or disclosure of derived results is subject to restrictions, as specified');
INSERT INTO adam_table(attribute, description) VALUES ('noTimelineObligations','There are no timeline restrictions');
INSERT INTO adam_table(attribute, description) VALUES ('whichTimelineObligations','The period of access has time limitations, as specified');
INSERT INTO adam_table(attribute, description) VALUES ('noSecurityObligations','There are no requirements regarding data security measures');
INSERT INTO adam_table(attribute, description) VALUES ('whichSecurityObligations','User must have adequate data security measures, as specified');
INSERT INTO adam_table(attribute, description) VALUES ('noExpungingObligations','There are no requirements regarding withdrawal, destruction or return of any subject data');
INSERT INTO adam_table(attribute, description) VALUES ('whichExpungingObligations','Some subject data must be withdrawn, destroyed or returned, as specified');
INSERT INTO adam_table(attribute, description) VALUES ('noLinkingObligations','There are no restrictions regarding the linking of accessed records to other datasets');
INSERT INTO adam_table(attribute, description) VALUES ('whichLinkingObligations','Accessed records may only be linked to other datasets, as specified');
INSERT INTO adam_table(attribute, description) VALUES ('noRecontactProvisions','There is no possibility of recontacting data subjects');
INSERT INTO adam_table(attribute, description) VALUES ('allowedRecontactProvisions','Subject recontact may occur in certain circumstances, as specified');
INSERT INTO adam_table(attribute, description) VALUES ('compulsoryRecontactProvisions','Subject recontact must occur in certain circumstances, as specified');
INSERT INTO adam_table(attribute, description) VALUES ('noIPClaimObligations','There are no restrictions regarding intellectual property claims based on use of the accessed resource');
INSERT INTO adam_table(attribute, description) VALUES ('whichIPClaimObligations','Options for intellectual property claims based on use of the accessed resources are limited, as specified');
INSERT INTO adam_table(attribute, description) VALUES ('noReportingObligations','There are no requirements to report back regarding use of the accessed resources');
INSERT INTO adam_table(attribute, description) VALUES ('whichReportingObligations','Reporting on use of the accessed resources may be required, as specified');
INSERT INTO adam_table(attribute, description) VALUES ('noPaymentObligations','No fees will be levied for access of the resources');
INSERT INTO adam_table(attribute, description) VALUES ('whichPaymentObligations','Fees may be levied for access of the resources, as specified');

CREATE TABLE beacon_dataset_adam_table(
	dataset_id character varying(50) NOT NULL,
	adam_id int NOT NULL REFERENCES adam_table(id),
	value_id int NOT NULL REFERENCES adam_value_table(id),
	PRIMARY KEY (dataset_id, adam_id)
);

CREATE TABLE beacon_dataset_adam_detailed_table(
	dataset_id character varying(50) NOT NULL,
	adam_id int NOT NULL REFERENCES adam_table(id),
	value character varying(200) NOT NULL,
	PRIMARY KEY (dataset_id, adam_id)
);

-----------------------------------
-------------- VIEWS --------------
-----------------------------------
CREATE OR REPLACE VIEW beacon_dataset_consent_code AS
SELECT dc.dataset_id,
	code.abbr AS code,
	code.description AS description,
	dc.detail,
	cat.name AS category
FROM beacon_dataset_consent_code_table dc
INNER JOIN consent_code_table code ON code.id=dc.consent_code_id
INNER JOIN consent_code_category_table cat ON cat.id=code.category_id
ORDER BY dc.dataset_id, cat.id, code.id
;

CREATE OR REPLACE VIEW beacon_dataset_adam AS
SELECT	subq.dataset_id,
	a.attribute,
	subq.value
FROM (
	SELECT da.dataset_id,
		da.adam_id,
		av.value
	FROM beacon_dataset_adam_table da
	INNER JOIN adam_value_table av ON av.id=da.value_id
	UNION
	SELECT detailed.dataset_id,
		detailed.adam_id,
		detailed.value
	FROM beacon_dataset_adam_detailed_table detailed
	ORDER BY dataset_id, adam_id
) subq
INNER JOIN adam_table a ON a.id=subq.adam_id
;
```
That will create the schema and also load some essential data for data use conditions.

You can also copy these lines into a file to load it as follows:
```
psql -h server_host -p server_port -d database_name -U user_name < schema_dump.sql
```

##Load the data
Use this script to parse a VCF input file:
```bash
#!/bin/bash
grep -v "#"| cut -f1,2,4,5,7 | sort | uniq | awk -v ds=$1 '
{ if ( (length($3) == 1 && length($4) == 1) && ($5 == "PASS" || $5 == ".")) print ds";"$1";"$2";"$4}
' > file.SNPs
```
* Copy the content into a file called vcf_parser.sh and give it executable rights:
```
chmod +x vcf_parser.sh
```
* Run this script executing:
```
./vcf_parser.sh dataset_id < file.vcf
```
This script will generate an output file called file.SNPs.

* Load the dataset information into **beacon_dataset_table**.
```sql
INSERT INTO beacon_dataset(id, description, access_type, reference_genome, size)
    VALUES ('dataset_id', 'dataset_description', 'i. e. PUBLIC', 'i. e. grch37', 123456);
```
* Load the generated file into **beacon_data_table**:
```
cat file.SNPs | psql -h server_host -p port -U user_name -c "COPY beacon_data_table(dataset_id,chromosome,position,alternate) FROM STDIN USING DELIMITERS ';' CSV" database_name
```

#Managing the code
##Download the project
Execute a git pull from the projects **elixir_beacon** (current one) and **elixir_core** located at the [Elixir' repository](https://github.com/elixirhub/human-data-beacon).

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
The key file is **/src/main/resources/application-{profile}.properties** (see [Deploy JAR](https://github.com/sdelatorrep/elixir_beacon/blob/master/README.md#deploy-the-jar) for more information about profiles).

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
 

Do the same changes in **/src/test/resources/application-{profile}.properties**. By default, the database there is called *elixir_beacon_testing*.

If you use a different DB than Postgres, you must add the corresponding library to the **/lib** folder inside the JAR (you don't need to recompile) or add the dependency to the pom.xml so maven can download the library (this will force you to compile).

You can also change the sample requests (*sampleAlleleRequests* field in the JSON) that are shown in the **/info** endpoint (see [Using the application](https://github.com/sdelatorrep/elixir_beacon/blob/master/README.md#using-the-application)) by modifying some values in **application-{profile}.yml** file:
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
That will also execute the tests. To skip them add <code>-Dmaven.test.skip=true</code> to the command.

To execute only the tests run:
```
mvn test
```
NOTE: To execute the tests you should have a different database than the main one (see [Create databases](https://github.com/sdelatorrep/elixir_beacon/blob/master/README.md#create-databases)).

If compilation and test execution are successful, a JAR file will be generated in the folder **/target** with the name **elixir-beacon-0.3.jar**

##Extend/Change functionality
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
            java -Dloader.path=lib/ -Dspring.profiles.active=test -jar elixir-beacon-0.3.jar
            ```

##Deploy the JAR
To deploy the JAR run:
 ```
java -jar elixir-beacon-0.3.jar --spring.profiles.active=test
 ```
It will generate a log in the file **application.log** located in the same folder where the JAR is located.

This argument <code>--spring.profiles.active=test</code> specifies the profile to be used. By default, there are 2 profiles: **dev** and **test**. Each profile will use its own set of properties files. 

I. e. **test** profile will use:
* application-test.properties
* application-test.yml

Using the default configuration, the application will be available at: **localhost:9075/elixirbeacon/v03/**

##Run integration tests
We use JMeter to run this kind of tests. We have an artifact called **elixir-beacon-service-tests**. To get the code execute a git pull from the elixir_beacon_tests project at [Elixir's Human Data Beacon repository](https://github.com/elixirhub/human-data-beacon).

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
* /info
* /query

They are defined in the **org.ega_archive.elixirbeacon.ElixirBeaconController** class.

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
The 3 examples that appear in field sampleAlleleRequests can be customized by modifying **application-{profile}.yml** as explained in [Configure application](https://github.com/sdelatorrep/elixir_beacon/blob/master/README.md#configure-application).

##/query
To actually ask the beacon for questions like "do you have any genomes with an 'A' at position 100,735 on chromosome 3?" And the answer will be yes or no.

https://egatest.crg.eu/elixir_demo_beacon/query?referenceName=1&position=179832996&assemblyId=GRCh37
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
