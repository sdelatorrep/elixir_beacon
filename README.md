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
Log in the DB and grant privileges to a normal user (that is, not a super user):
psql database_name -U super_user
GRANT ALL PRIVILEGES ON DATABASE database_name TO normal_user;
Load the schema dump into the DB (download it here):
psql -h server_host -p server_port -d database_name -U user_name < schema_dump.sql
You can skip step 2) and load the schema using a super user in step 3) and after that granting privileges to a normal user (this user will be used by the application to connect to the database).
Create a second database (i. e. elixir_beacon_testing) that will be used in the tests.
