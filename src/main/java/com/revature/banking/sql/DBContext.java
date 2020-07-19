package com.revature.banking.sql;

import java.sql.Connection;

public interface DBContext {
	public Connection getConnection();
}
