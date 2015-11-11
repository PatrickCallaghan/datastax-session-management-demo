create keyspace if not exists datastax_session_management_demo WITH replication = {'class': 'NetworkTopologyStrategy', 'Solr': 1};

use datastax_session_management_demo; 

create table ticket( 
	id text PRIMARY KEY,
	data text, 
	last_updated timestamp
) WITH default_time_to_live = 172,800;  //2 days

create table ticket_cleaner_lasttime(
	id text PRIMARY KEY,
	last_updated timestamp
);

INSERT into ticket_cleaner_lasttime('dummy', '2015-11-11 11:00:00');

create table ticket_cleaner (
	expiry_type text,
	date_bucket text,
	