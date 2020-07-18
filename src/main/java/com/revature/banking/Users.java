package com.revature.banking;

import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Users implements DBContext {
	public Connection getConnection() {
		return DBConnectionManager.getConnection();
	}
	
	int userId, roleId;
	String userName, passWord, firstName, lastName, eMail;
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public int getRoleId() {
		return roleId;
	}
	public void setRoleId(int roleId) {
		this.roleId = roleId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassWord() {
		return passWord;
	}
	public void setPassWord(String passWord) {
		this.passWord = passWord;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getEMail() {
		return eMail;
	}
	public void setEMail(String eMail) {
		this.eMail = eMail;
	}
	
	public int insert (String un, String pw,
			String fn, String ln, String em, Roles role) throws Exception {
		String sql = "INSERT INTO "+
			"users(username, password, first_name, last_name, email, role) "+
			"VALUES (?,?,?,?,?,?::Roles) ON CONFLICT DO NOTHING RETURNING user_id;";
		PreparedStatement ps = getConnection().prepareStatement(sql);
		int i = 0;
		ps.setString	(++i, un);
		ps.setString	(++i, pw);
		ps.setString	(++i, fn);
		ps.setString	(++i, ln);
		ps.setString	(++i, em);
		ps.setString	(++i, role.toString());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) { i = rs.getInt("user_id"); System.out.println(i); }
		return i;
	}
	
	public boolean login(String un, String pw) throws Exception {
		final String sql = "SELECT password FROM users WHERE username = ?;";
		PreparedStatement ps = getConnection().prepareStatement(sql);
		ps.setString(1, un);
		ResultSet rs = ps.executeQuery();
		return pw == rs.getString("password");
	}
		
	public static void main(String[] args) throws Exception {
    	DBConnectionManager.initialize(args[0]);
    	System.out.println(new Users().insert("cdefghi", "password",
    			"Donald", "Trump", "potus@executive.gov", Roles.standard));
	}
	
}
