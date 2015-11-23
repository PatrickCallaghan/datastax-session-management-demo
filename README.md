Session Management Demo
========================

This demo tries to replicate a session management system. The business rules of the system are 

1. A ticket is created when a user enters a site. 

2. Each ticket has a max inactivity time of 10 mins

3. Each time there is an interaction, the ticket is extended another 10 mins

4. If a user has declared that they want to kept logged in, the ticket has a max life of 1 day

5. If a user has declared that they do not want to kept logged in, the ticket has a max life of 1 hour

6. A ticket must not be valid after its declared time. 

7. I need to know the id of a ticket when it expires (due to max life or inactivity) so I can do something with it.

We can do this by saying
	
If its keep_logged_in ticket - then use a ttl of 1 day
If its not a keep_logged_in ticket - then use a ttl of 1 hours
Every time a ticket is presented, updated the ticket with a last seen time of 10 mins.

A ticket_cleaner will be used to hold the all the tickets in the system and when we expect them to be ready for expiry. 

A cleaner will be used to ensure that all tickets are removed. 

## Schema Setup
Note : This will drop the keyspace "datastax_session_management_demo" and create a new one. All existing data will be lost. 

To specify contact points use the contactPoints command line parameter e.g. 

	'-DcontactPoints=192.168.25.100,192.168.25.101'
	
The contact points can take mulitple points in the IP,IP,IP (no spaces).

To create the a single node cluster with replication factor of 1 for standard localhost setup, run the following

To create the schema, run the following

	mvn clean compile exec:java -Dexec.mainClass="com.datastax.demo.SchemaSetup" -DcontactPoints=localhost
	
To create some tickets and start the cleaner, run the following 
	
	mvn clean compile exec:java -Dexec.mainClass="com.datastax.session.Main" -DcontactPoints=localhost
	
To remove the tables and the schema, run the following.

	mvn clean compile exec:java -Dexec.mainClass="com.datastax.demo.SchemaTeardown" -DcontactPoints=localhost
    
    
