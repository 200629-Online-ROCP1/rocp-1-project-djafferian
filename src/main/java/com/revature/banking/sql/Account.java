package com.revature.banking.sql;

import java.sql.Connection;

public class Account extends Table implements DBContext {
	public Connection getConnection() {
		return DBConnectionManager.getConnection();
	}

	public Account() {
		// TODO Auto-generated constructor stub
	}

}
