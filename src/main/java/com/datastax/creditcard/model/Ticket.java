package com.datastax.creditcard.model;

import java.util.Date;
import java.util.Map;

public class Ticket {

	private String id;
	private String data;
	private Date lastUpdated;
	
	public Ticket(String id, String data, Date lastUpdated) {
		super();
		this.id = id;
		this.data = data;
		this.lastUpdated = lastUpdated;
	}
	public String getId() {
		return id;
	}
	public String getData() {
		return data;
	}
	public Date getLastUpdated() {
		return lastUpdated;
	}
	@Override
	public String toString() {
		return "Ticket [id=" + id + ", data=" + data + ", lastUpdated=" + lastUpdated + "]";
	}
}
