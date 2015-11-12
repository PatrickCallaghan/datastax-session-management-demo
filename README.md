Session Management Demo
========================

This demo tries to replicate a session management system. The business rules of the system are 

1. A ticket is created when a user enters a site. 

2. Each ticket has a max inactivity time of 10 mins

3. Each time there is an interaction, the ticket is extended another 10 mins

4. If a user has declared that they want to kept logged in, the ticket has a max life of 1 day

5. If a user has declared that they do not want to kept logged in, the ticket has a max life of 4 hours

6. A ticket must not be valid after its declared time. 

We can do this by saying
	
If its keep_logged_in ticket - then use a ttl of 1 day

If its not a keep_logged_in ticket - then use a ttl of 4 hours

Every time a ticket is presented, updated the ticket with a ttl of 10 mins.

A cleaner will be used to ensure that all tickets are removed. 

To create the schema, run the following

	mvn clean compile exec:java -Dexec.mainClass="com.datastax.demo.SchemaSetup"
	
To create some transactions, run the following 
	
	mvn clean compile exec:java -Dexec.mainClass="com.datastax.session.Main"
	
	mvn clean compile exec:java -Dexec.mainClass="com.datastax.session.RunCleaner" 

To remove the tables and the schema, run the following.

    mvn clean compile exec:java -Dexec.mainClass="com.datastax.demo.SchemaTeardown"
    
    