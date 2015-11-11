Transaction Search Demo
========================

This requires DataStax Enterprise running in Solr mode.

This demo shows 3 ways to search transactions in a users account. In this demo we will presume that users can enter notes for each transaction on their account and then they will want to search by some or all of these notes.

The 3 different ways of querying will be 
1. SolrJ

2. CQL using a partition restriction and the solr_query clause

3. CQL using a partition restriction and searching the transactions in java code from the client.


To create the schema, run the following

	mvn clean compile exec:java -Dexec.mainClass="com.datastax.demo.SchemaSetup"
	
To create some transactions, run the following 
	
	mvn clean compile exec:java -Dexec.mainClass="com.datastax.creditcard.Main" 

You can the following to change the default no of transactions and credit cards 
	
	-DnoOfTransactions=10000000 -DnoOfCreditCards=1000000
	
To create the solr core, run 

	bin/dsetool create_core datastax_transaction_search_demo.latest_transactions generateResources=true reindex=true coreOptions=rt.yaml
	
To run the requests run the following 
	
	mvn clean compile exec:java -Dexec.mainClass="com.datastax.creditcard.RunRequests"
	
To change the no of requests add the following

	-DnoOfRequests=100000 -DnoOfCreditCards=1000000	
	
To remove the tables and the schema, run the following.

    mvn clean compile exec:java -Dexec.mainClass="com.datastax.demo.SchemaTeardown"
    
    