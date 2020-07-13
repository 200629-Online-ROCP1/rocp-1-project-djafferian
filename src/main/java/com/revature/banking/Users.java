package com.revature.banking;

import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;

public class Users {
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
	
	public boolean insert (int uid, int rid, String un, String pw,
							String fn, String ln, String em);
		int i = 0;
		String sql = "INSERT INTO users(role_id, username, password, " +
						"firstName, lastName, email VALUES (?,?,?,?,?,?) " +
						"RETURNING user_id;"";
		PreparedStatement ps = PreparedStatement(sql);
		ps.setInt		(++i, rid);
		ps.setString	(++i, un);
		ps.setString	(++i, pw);
		ps.setString	(++i, fn);
		ps.setString	(++i, ln);
		ps.setString	(++i, em);
		
		return ps.execute();
}
	
	public boolean login(String un, String pw) {
		final String sql = "SELECT password FROM users WHERE username = ?;";
		Connection conn = DBConnectionManager.getConnection();
		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setString(1, un);
		ResultSet rs = ps.executeQuery();
		return pw == rs.getString("password");
	}
		
	public	
	}
	
}
