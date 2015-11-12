create keyspace if not exists datastax_session_management_demo WITH replication = {'class': 'NetworkTopologyStrategy', 'Cassandra': 1};

use datastax_session_management_demo; 

create table if not exists ticket( 
	id text PRIMARY KEY,
	data text, 
	last_updated timestamp
) WITH default_time_to_live = 172800;  

create table if not exists ticket_cleaner_lasttime(
	id text PRIMARY KEY,
	last_updated timestamp
);

INSERT into ticket_cleaner_lasttime(id, last_updated) values ('dummy', '2015-11-11 11:00:00');

create table if not exists ticket_cleaner (
	expiry_type text,
	date_bucket text,
	id text,
	PRIMARY KEY ((expiry_type, date_bucket), id)
);