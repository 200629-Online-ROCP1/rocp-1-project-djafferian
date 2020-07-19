package com.revature.banking.sql;

import java.sql.Connection;

public enum Types implements DBContext {
	checking, savings;
	
	public Connection getConnection() {
		return DBConnectionManager.getConnection();
	}
}