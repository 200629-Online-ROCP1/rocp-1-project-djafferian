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
	Users() throws Exception {
		super("users","user_id");
	}
/*
	public int insert (String un, String pw,
			String fn, String ln, String em, Roles role) throws Exception {
		String sql = "INSERT INTO "+
			"users(username, password, first_name, last_name, email, role) "+
			"VALUES (?,?,?,?,?,?::Roles) ON CONFLICT (username) DO NOTHING "+
			"RETURNING user_id;";
		PreparedStatement ps = getConnection().prepareStatement(sql);
		int i = 0;
		ps.setString	(++i, un);
		ps.setString	(++i, pw);
		ps.setString	(++i, fn);
		ps.setString	(++i, ln);
		ps.setString	(++i, em);
		ps.setString	(++i, role.toString());
		ResultSet rs = ps.executeQuery();
		return rs.next() ? rs.getInt("user_id") : 0;
	}
	
	public LinkedHashMap select (int user_id) {
		String sql = "SELECT * FROM users WHERE user_id = ?;";
		PreparedStatement ps = getConnection().prepareStatement(sql);
		ps.setInt(1, user_id);
		ResultSet rs = ps.executeQuery();
		if (!rs.next()) return null;
		ResultSetMetaData rsmd = rs.getMetaData();
		LinkedHashMap<String,Object> lhm = new LinkedHashMap<String,Object>(rsmd.getColumnCount());
		int i = 0;
		do {
			lhm.put(rsmd.getColumnName(++i),);
		} while(next());
		Map<Integer,Object> map = HashMap
		i = 0;	// if there is no row returned
		while (rs.next()) i = rs.getInt("user_id");
		return i;

	}
*/	
	public boolean login(String un, String pw) throws Exception {
		final String sql = "SELECT password FROM users WHERE username = ?;";
		PreparedStatement ps = getConnection().prepareStatement(sql);
		ps.setString(1, un);
		ResultSet rs = ps.executeQuery();
		return pw == rs.getString("password");
	}
		
	public static void main(String[] args) throws Exception {
    	DBConnectionManager.initialize(args[0]);
    	Users users = new Users();
    	Row row = users.getRow();
    	row.put("username","fghijkl");
    	row.put("password","password");
    	row.put("first_name","Donald");
    	row.put("last_name","Trump");
    	row.put("email","potus@executive.gov");
    	row.put("role",Roles.standard);
    	int user_id = users.insert(row);
    	System.out.println(user_id);
    	row = users.select(user_id);
    	System.out.println(row.get("username"));
	}
	
}
