package com.revature.banking.sql;

import java.sql.Connection;

public class AccountUsers extends Table implements DBContext {
	public Connection getConnection() {
		return DBConnectionManager.getConnection();
	}

	public AccountUsers() {
		// TODO Auto-generated constructor stub
	}

}
