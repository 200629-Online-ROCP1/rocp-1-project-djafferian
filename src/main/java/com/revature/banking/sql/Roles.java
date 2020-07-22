package com.revature.banking.sql;

import java.sql.Connection;

public enum Roles implements DBContext {
	standard, premium, employee, administrator;
	
	public Connection getConnection() {
		return DBConnectionManager.getConnection();
	}
	
	public static void main(String[] args) {
		Roles[] roles = Roles.values();
		for (int i=0; i<roles.length; i+=1) 
		    System.out.println((i+1) + " " + roles[i].toString());
		String sql = "SELECT * FROM Roles;";
	}
}
