package com.datastax.session;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.demo.utils.PropertyHelper;
import com.datastax.session.dao.SessionManagementDao;
import com.datastax.session.model.Ticket;

public class Main {

	private static Logger logger = LoggerFactory.getLogger(Main.class);
	
	public Main() {

		String contactPointsStr = PropertyHelper.getProperty("contactPoints", "localhost");
		String noOfTicketsStr = PropertyHelper.getProperty("noOfTickets", "10000000");
		
		final Map<String, Ticket> existingTicketIds = new HashMap<String, Ticket>();
		final SessionManagementDao dao = new SessionManagementDao(contactPointsStr.split(","));		
		final int noOfTickets = Integer.parseInt(noOfTicketsStr);
		
		final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		
		scheduledExecutor.scheduleWithFixedDelay(new Runnable(){
			@Override
			public void run() {
				logger.info("Starting clean");
				List<String> deletedTicketsId = dao.runCleaner();
				
				for(String id : deletedTicketsId){
					existingTicketIds.remove(id);
				}
			}
			
		}, 5, 60, TimeUnit.SECONDS);	

		logger.info("Starting ticket generation.");
		
		while (true){
			Ticket ticket = createRandomTicket(noOfTickets);
			
			//Only update if we have already seen this ticket and its not a keep logged in.  
			if (existingTicketIds.containsKey(ticket.getId()) 
					&& !existingTicketIds.get(ticket.getId()).isKeepLoggedIn()){
				
				logger.debug("Processing existing ticket " + ticket);
				boolean updated = dao.updateTicket(ticket.getId());
				
				//If not updated - ticket does not exist or is expired.
				if (!updated){
					logger.info("Ticket has expired");
					dao.insertNewTicket(ticket);
				}
			}else{
				dao.insertNewTicket(ticket);
			}
			
			existingTicketIds.put(ticket.getId(), ticket);
			sleep (1);
		}				
	}

	private Ticket createRandomTicket(int noOfTickets) {
		int ticketNo = new Double(Math.ceil(Math.random() * noOfTickets)).intValue();
		Ticket ticket = new Ticket(ticketNo +"", DateTime.now().toString(), new Date());
		
		//20% of the time
		if ((Math.random() * 5) < 1){
			ticket.setKeepLoggedIn(true);
		}
		
		return ticket;
	}

	private void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Main();

		System.exit(0);
	}
}
