package com.revature.banking.servlets;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.banking.sql.Roles;
import com.revature.banking.sql.Row;
import com.revature.banking.sql.Users;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class UsersServlet extends HttpServlet {
	public static final ObjectMapper om = new ObjectMapper();

	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws IOException, ServletException {
		res.setContentType("application/json");
		res.setStatus(404);	// Presume failure
		final String URI = req.getRequestURI();
		String[] portions = URI.split("/");
		Integer userId = null;
		switch (portions.length) {
		case 4:
			try {
				userId = Integer.valueOf(portions[3]);
			} catch (NumberFormatException nfe) {
				return;
			}
		case 3:
			if (portions[2].equals("users")) break;
			HttpSession session = req.getSession(false);
			if (session == null) JSONTools.securityBreach(req, res);
			Roles role = (Roles)session.getAttribute("role");
			if (role !=  Roles.administrator && role != Roles.employee) {
				JSONTools.securityBreach(req, res);
				return;
			}
		default:
			return;
		}
		
		try {
			HttpSession session = req.getSession(false);
			if (session == null) {
				JSONTools.securityBreach(req, res);
				return;
			}
			Roles role = (Roles)session.getAttribute("role");
			if (role !=  Roles.administrator && role != Roles.employee &&
					userId != (Integer)session.getAttribute("user_id")) {
				JSONTools.securityBreach(req, res);
				return;
			}
			Users users = new Users();
			JSONTools.dispenseJSON(res,
					userId == null ? users.readAll():
					users.readOne(userId.intValue()));
			res.setStatus(200);
	    } catch (SQLException ex) {
	    	// handle any exception
	    	System.err.println("SQLException: " + ex.getMessage());
	    	System.err.println("SQLState: " + ex.getSQLState());
	    	System.err.println("VendorError: " + ex.getErrorCode());
			ex.printStackTrace();
			res.setStatus(501);
			return;
		}
	}
	
	protected void doPut(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		res.setContentType("application/json");
		res.setStatus(400);	// Presume failure
		final String URI = req.getRequestURI();
		String[] portions = URI.split("/");
		Integer userId = null;
		switch (portions.length) {
		case 3:
			if (portions[2].equals("users")) break;
		default:
			return;
		}
		
		try {
			Users users = new Users();
			Row row = users.getRow();
			if (!JSONTools.receiveJSON(req, row)) return;
			
			HttpSession session = req.getSession(false);
			if (session == null) {
				JSONTools.securityBreach(req, res);
				return;
			}
			Roles role = (Roles)session.getAttribute("role");
			if (role !=  Roles.administrator &&
					row.get("user_id") != (Integer)session.getAttribute("user_id")) {
				JSONTools.securityBreach(req, res);
				return;
			}
			
			users.update(row);
			res.setStatus(200);
		} catch (SQLException ex) {
	    	// handle any exception
	    	System.err.println("SQLException: " + ex.getMessage());
	    	System.err.println("SQLState: " + ex.getSQLState());
	    	System.err.println("VendorError: " + ex.getErrorCode());
			ex.printStackTrace();
			res.setStatus(501);
			return;
		}
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		res.setContentType("application/json");
		res.setStatus(400);	// Presume failure
		final String URI = req.getRequestURI();
		String[] portions = URI.split("/");
		if (portions.length != 3) return;
		if (portions[2].equals("logout")) {
			HttpSession session = req.getSession(false);
			if (session != null) {
				Object username = session.getAttribute("username");
				session.invalidate();
				JSONTools.dispenseJSONMessage(res,
						"You have successfully logged out "+username);
				res.setStatus(200);
			} else {
				JSONTools.dispenseJSONMessage(res,
						"There was no user logged into the session");
				res.setStatus(400);				
			}
			return;
		}
		try {
			Users users = new Users();
			Row row = users.getRow();
			if (portions[2].equals("login")) {
				if (req.getSession(false) != null) {
					JSONTools.securityBreach(req, res);
					return;
				}
				Map<String,Object> credentials = new HashMap<String,Object>();
				credentials.put("username","");
				credentials.put("password","");
				if (JSONTools.receiveJSON(req, credentials)) {
					ArrayList<Row> rows = users.readSome("username",credentials.get("username"));
					if (1 < rows.size()) { res.setStatus(500); return; }
					if (1 == rows.size()) {
						row = rows.get(0);
						if (credentials.get("password").equals(row.get("password"))) {
							HttpSession session = req.getSession();
							session.setAttribute("user_id", row.get("user_id"));
							session.setAttribute("username",row.get("username"));
							session.setAttribute("role", row.get("role"));
							JSONTools.dispenseJSON(res, row);
							res.setStatus(200);
							return;
						}
					}
				}
				JSONTools.dispenseJSONMessage(res, "Invalid Credentials");
				res.setStatus(400);
			}
			if (portions[2].equals("register")) {
				HttpSession session = req.getSession(false);
				if (session == null ||
						session.getAttribute("role") != Roles.administrator) {
					JSONTools.securityBreach(req, res);
					return;
				}
				if (JSONTools.receiveJSON(req, row)) {
					row.put("user_id",0);	// Necessary ?
					int user_id = users.create(row);
					if (0 < user_id) {
						JSONTools.dispenseJSON(res, users.readOne(user_id));
						res.setStatus(201);
						return;
					}
				}
				JSONTools.dispenseJSONmessage(res, "Invalid fields");
				res.setStatus(400);
			}
		} catch (SQLException ex) {
	    	// handle any exception
	    	System.err.println("SQLException: " + ex.getMessage());
	    	System.err.println("SQLState: " + ex.getSQLState());
	    	System.err.println("VendorError: " + ex.getErrorCode());
			ex.printStackTrace();
			res.setStatus(501);
		}
	}
}
