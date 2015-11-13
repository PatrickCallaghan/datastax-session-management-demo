package com.datastax.session.dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.session.model.Expiry;
import com.datastax.session.model.Ticket;

public class SessionManagementDao {

	private static Logger logger = LoggerFactory.getLogger(SessionManagementDao.class);
	private static final int DAY = 85000;
	private static final int HOUR = 3600;
	private static final int MINS_10 = 600;
	private Session session;

	private DateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd HHmm");
	private static String keyspaceName = "datastax_session_management_demo";

	private static String ticketTable = keyspaceName + ".ticket";
	private static String lastCleanerTimeTable = keyspaceName + ".ticket_cleaner_lasttime";
	private static String ticketCleanerTable = keyspaceName + ".ticket_cleaner";
	
	private String INSERT_TICKET = "insert into " + ticketTable + " (id, data, last_updated) values (?, ?, ?) using TTL ?";
	private String DELETE_TICKET = "delete from " + ticketTable + " where id = ?";
	private String SELECT_TICKET = "select id, data, last_updated from " + ticketTable + " where id = ?";
	private String SELECT_ID_FROM_TICKET = "select id from " + ticketTable + " where id = ?";
	
	private String INSERT_TICKET_TO_CLEANER = "insert into " + ticketCleanerTable + " (expiry_type, date_bucket, id) values (?, ?, ?)";
	private String SELECT_FROM_CLEANER = "select id from " + ticketCleanerTable + " where expiry_type = ? and date_bucket = ?";
	
	private String INSERT_TICKET_LASTUPDATED = "INSERT INTO " + lastCleanerTimeTable + " (id, last_updated) values ('dummy', ?)";
	private String SELECT_CLEANER_LASTUPDATED = "select last_updated from  " + lastCleanerTimeTable + " where id = 'dummy'";
		
	private PreparedStatement insertTicketStmt;
	private PreparedStatement selectTicketStmt;
	private PreparedStatement selectIdTicketStmt;
	private PreparedStatement deleteTicketStmt;
	
	private PreparedStatement insertTicketToCleanerStmt;
	private PreparedStatement selectFromCleanerStmt;
	
	private PreparedStatement insertLastTime;
	private PreparedStatement selectLastTime;
	

	public SessionManagementDao(String[] contactPoints) {

		Cluster cluster = Cluster.builder().addContactPoints(contactPoints).build();

		this.session = cluster.connect();
		
		this.selectTicketStmt = session.prepare(SELECT_TICKET);
		this.selectIdTicketStmt = session.prepare(SELECT_ID_FROM_TICKET);
		this.insertTicketStmt = session.prepare(INSERT_TICKET);
		this.deleteTicketStmt = session.prepare(DELETE_TICKET);
		
		this.insertTicketToCleanerStmt = session.prepare(INSERT_TICKET_TO_CLEANER);		
		this.selectFromCleanerStmt = session.prepare(SELECT_FROM_CLEANER);
		
		this.insertLastTime = session.prepare(INSERT_TICKET_LASTUPDATED);
		this.selectLastTime = session.prepare(SELECT_CLEANER_LASTUPDATED);
	}

	public void insertNewTicket(Ticket ticket){
		
		AsyncWriterWrapper wrapper = new AsyncWriterWrapper();		
		int ttl = ticket.isKeepLoggedIn() ? DAY : HOUR;
		
		wrapper.addStatement(this.insertTicketStmt.bind(ticket.getId(), ticket.getData(), ticket.getLastUpdated(), ttl));
		
		if (ticket.isKeepLoggedIn()){
			//If keeping login - then expire in 1 day
			String expiryDate = dateFormatter.format(new DateTime().plusDays(1).toDate());
			wrapper.addStatement(this.insertTicketToCleanerStmt.bind(Expiry.FULL.name(), expiryDate, ticket.getId()));
		
		}else{
			//If not keeping login - then expire in 1 hour
			String expiryDate = dateFormatter.format(new DateTime().plusMinutes(60).toDate());			
			wrapper.addStatement(this.insertTicketToCleanerStmt.bind(Expiry.SOFT.name(), expiryDate, ticket.getId()));
		}
		
		while(!wrapper.exhausted()){
			if (!wrapper.executeAsync(session)){
				logger.info("Exception : " + wrapper.getException());
			}else{
				return;
			}			
		}		
	}
	
	//Create a new expiry date for the ticket. 
	public boolean updateTicket(String ticketId){
		
		//Check if ticket exists
		ResultSet resultSet = this.session.execute(this.selectIdTicketStmt.bind(ticketId));
		
		if (resultSet == null || resultSet.one() == null){
			logger.info("Ticket with id " + ticketId + " has expired.");
			return false;
		}
		
		//Update the expiry date 
		String expiryDate = dateFormatter.format(new DateTime().plusMinutes(10).toDate());
		
		this.session.execute(this.insertTicketToCleanerStmt.bind(Expiry.SOFT.name(), expiryDate, ticketId));
		
		return true;
	}
	
	public List<String> runCleaner(){
		
		//Get last cleaned time
		DateTime lastClean = new DateTime(session.execute(selectLastTime.bind()).one().getDate("last_updated"));		
		DateTime latestMinute = DateTime.now().minusMinutes(1);
		List<String> deletedTicketIds = new ArrayList<String>();
		
		while (lastClean.isBefore(latestMinute)){
			
			logger.info("Cleaning for " + lastClean);
			
			//Get all tickets that are eligible to expire - Full clean - must be expired
			List<String> hardCleanTickets = this.getAllExpiryTickets(lastClean, Expiry.FULL.name());
			deletedTicketIds = this.hardCleanTickets(hardCleanTickets);
			
			//Get all tickets that are eligible to expire - only delete if time hasn't been updated in 15 minutes
			List<String> tickets = this.getAllExpiryTickets(lastClean, Expiry.SOFT.name());
			deletedTicketIds.addAll(this.softCleanTickets(tickets));
						
			//Move on to next minute and save
			lastClean = lastClean.plusMinutes(1);
			session.execute(insertLastTime.bind(lastClean.toDate()));
		}
		
		return deletedTicketIds;
	}
	
	private List<String> hardCleanTickets(List<String> hardCleanTickets){
		AsyncWriterWrapper hardCleaner = new AsyncWriterWrapper();
		List<String> ticketIds = new ArrayList<String>();
		
		for (String id : hardCleanTickets){
			hardCleaner.addStatement(this.deleteTicketStmt.bind(id));
			
			//Do something else with the ticket id if necessary.
			ticketIds.add(id);
		}
		
		logger.info("Cleaning HARD - " + hardCleanTickets.size() + " tickets.");
		
		hardCleaner.executeAsync(session);
		
		while(!hardCleaner.exhausted()){
			if (!hardCleaner.executeAsync(session)){
				logger.info("Exception : " + hardCleaner.getException());
			}else{
				return ticketIds;
			}			
		}
		return ticketIds;
	}
	
	private List<String> softCleanTickets(List<String> softCleanTickets){
		AsyncWriterWrapper softCleaner = new AsyncWriterWrapper();
		
		List<String> ticketIds = new ArrayList<String>();
		for (String id : softCleanTickets){
			
			Ticket ticket = selectTicketById(id);
			
			if(ticket!=null){
				
				//If ticket hasn't been used in over an hour
				if (DateTime.now().isAfter(new DateTime(ticket.getLastUpdated()))){
					softCleaner.addStatement(this.deleteTicketStmt.bind(id));
					
					//Do something else with the ticket id if necessary.
					ticketIds.add(id);
				}									
			}			
		}
		logger.info("Cleaning SOFT - " + softCleaner.getStatementCounter() + " of " + softCleanTickets.size() + " tickets.");
		softCleaner.executeAsync(session);
		
		while(!softCleaner.exhausted()){
			if (!softCleaner.executeAsync(session)){
				logger.info("Exception : " + softCleaner.getException());
			}else{
				return ticketIds;
			}			
		}
		return ticketIds;
	}
	
	public Ticket selectTicketById(String id){
		
		ResultSet resultSet = this.session.execute(this.selectTicketStmt.bind(id));
		
		Row row = resultSet.one();
		if (row != null){
			return new Ticket(row.getString("id"), row.getString("data"), row.getDate("last_updated"));
		}
		return null;
	}

	private List<String> getAllExpiryTickets(DateTime lastClean, String expiryType) {
		List<String> tickets = new ArrayList<String>();
		
		String bucket = dateFormatter.format(lastClean.toDate());	
		ResultSet results = this.session.execute(this.selectFromCleanerStmt.bind(expiryType, bucket));
		
		Iterator<Row> iter = results.iterator();
		
		while (iter.hasNext()){
			Row row = iter.next();
			tickets.add(row.getString("id"));
		}
		
		return tickets;
	}
	
}
