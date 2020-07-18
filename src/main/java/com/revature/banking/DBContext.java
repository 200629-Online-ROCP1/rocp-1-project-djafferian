package com.revature.banking;

import java.sql.Connection;

public interface DBContext {
	public Connection getConnection();
}
