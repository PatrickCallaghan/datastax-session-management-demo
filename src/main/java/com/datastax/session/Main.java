package com.datastax.session;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.creditcard.model.Ticket;
import com.datastax.demo.utils.PropertyHelper;
import com.datastax.session.dao.SessionManagementDao;

public class Main {

	private static Logger logger = LoggerFactory.getLogger(Main.class);

	public Main() {

		String contactPointsStr = PropertyHelper.getProperty("contactPoints", "localhost");
		String noOfTicketsStr = PropertyHelper.getProperty("noOfTickets", "10000000");
		
		Set<String> existingTicketIds = new HashSet<String>();
		final SessionManagementDao dao = new SessionManagementDao(contactPointsStr.split(","));		
		final int noOfTickets = Integer.parseInt(noOfTicketsStr);
		final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		
		scheduledExecutor.scheduleWithFixedDelay(new Runnable(){

			@Override
			public void run() {
				logger.info("Cleaning at " + DateTime.now().toString());
				dao.runCleaner();
			}
			
		}, 1, 1, TimeUnit.MINUTES);
		
		
		while (true){
			Ticket ticket = createRandomTicket(noOfTickets);
			
			if (existingTicketIds.contains(ticket.getId())){
				dao.updateTicket(ticket.getId(), new Date());
			}else{
				dao.insertNewTicket(ticket);
			}
			
			sleep (1);
		}
	}

	private Ticket createRandomTicket(int noOfTickets) {
		int ticketNo = new Double(Math.ceil(Math.random() * noOfTickets)).intValue();
		
		return new Ticket(ticketNo +"", DateTime.now().toString(), new Date());
	}

	private void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
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
