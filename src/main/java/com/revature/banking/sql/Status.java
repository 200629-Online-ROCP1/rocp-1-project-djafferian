package com.revature.banking.sql;

import java.sql.Connection;

public enum Status implements DBContext {
	pending, open, closed, denied;
	
	public Connection getConnection() {
		return DBConnectionManager.getConnection();
	}
}