package com.revature.banking.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class AccountUsers extends Table implements DBContext {
	public Connection getConnection() {
		return DBConnectionManager.getConnection();
	}
	
	public AccountUsers() throws SQLException {
		super("account_users","account_users_id");
	}
	
	public boolean accountOwner(int account_id, int user_id) throws SQLException {
		ArrayList<Row> rows = readSome("user_id", Integer.valueOf(user_id));
		for (int i=0; i<rows.size(); i+=1)
			if (rows.get(i).get("account_id") == Integer.valueOf(account_id)) return true;
		return false;
	}
}
