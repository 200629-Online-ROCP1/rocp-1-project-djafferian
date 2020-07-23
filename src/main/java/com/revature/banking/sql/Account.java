package com.revature.banking.sql;

import java.sql.Connection;
import java.sql.SQLException;

public class Account extends Table implements DBContext {
	public Connection getConnection() {
		return DBConnectionManager.getConnection();
	}
	
	public Account() throws SQLException {
		super("account","account_id");
	}

	public static void main(String[] args) throws Exception {
		DBConnectionManager.initialize(args[0]);
		Account account = new Account();
		Row row = account.getRow();
		row.put("status",Statuses.pending);
		row.put("type",Types.savings);
		int account_id = account.create(row);
		System.out.println(account_id);
		row = account.readOne(account_id);
		System.out.println(row.get("status"));
		row.put("status",Statuses.open);
		row.put("type",Types.checking);
		account.update(row);
		row = account.readOne(account_id);
		System.out.println(row.get("type"));
		account.delete(account_id-1);
		row = account.readOne(account_id-1);
		System.out.println(row);
	}
}