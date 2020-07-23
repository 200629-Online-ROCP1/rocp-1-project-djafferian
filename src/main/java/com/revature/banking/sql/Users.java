package com.revature.banking.sql;

import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

public class Users extends Table implements DBContext {
	public Connection getConnection() {
		return DBConnectionManager.getConnection();
	}
	
	public Users() throws ServletException {
		super("users","user_id");
	}

	public static void main(String[] args) throws Exception {
    	DBConnectionManager.initialize(args[0]);
    	Users users = new Users();
    	Row row = users.getRow();
    	row.put("username","yzABCDEF");
    	row.put("password","password");
    	row.put("first_name","Donald");
    	row.put("last_name","Trump");
    	row.put("email","potus@executive.gov");
    	row.put("role",Roles.standard);
    	int user_id = users.create(row);
    	System.out.println(user_id);
    	row = users.readOne(user_id);
    	System.out.println(row.get("username"));
    	row.put("last_name","Trumpet");
    	row.put("role",Roles.premium);
    	users.update(row);
    	row = users.readOne(user_id);
    	System.out.println(row.get("role"));
    	users.delete(user_id-1);
    	row = users.readOne(user_id-1);
    	System.out.println(row);
	}
}
