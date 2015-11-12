package com.datastax.session;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.demo.utils.PropertyHelper;
import com.datastax.session.dao.SessionManagementDao;

public class RunCleaner {

	private static Logger logger = LoggerFactory.getLogger(RunCleaner.class);

	public RunCleaner() {

		String contactPointsStr = PropertyHelper.getProperty("contactPoints", "localhost");
		
		final SessionManagementDao dao = new SessionManagementDao(contactPointsStr.split(","));		
		final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		
		scheduledExecutor.scheduleWithFixedDelay(new Runnable(){

			@Override
			public void run() {
				dao.runCleaner();
			}
			
		}, 5, 60, TimeUnit.SECONDS);	
		
		while(true);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new RunCleaner();

		System.exit(0);
	}
}
