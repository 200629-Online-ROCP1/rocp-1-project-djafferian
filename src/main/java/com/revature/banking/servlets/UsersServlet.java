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
		default:
			return;
		}
		
		try {
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
			users.update(row);
			res.setStatus(200);
		} catch (Exception ex) {
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
				res.getWriter().print("{ \"message\": "+
						"\"You have successfully logged out "+username+"\" }");
				res.setStatus(200);
			} else {
				res.getWriter().print("{ \"message\": "+
						"\"There was no user logged into the session\" }");
				res.setStatus(400);				
			}
			return;
		}
		try {
			Users users = new Users();
			Row row = users.getRow();
			if (portions[2].equals("login")) {
				Map<String,Object> credentials = new HashMap<String,Object>();
				credentials.put("username","");
				credentials.put("password","");
				if (JSONTools.receiveJSON(req, credentials)) {
					ArrayList<Row> rows = users.readSome("username",credentials.get("username"));
					if (1 < rows.size()) { res.setStatus(500); return; }
					if (1 == rows.size()) {
						row = rows.get(0);
						if (row.get("password").equals(credentials.get("password"))) {
							JSONTools.dispenseJSON(res, row);
							HttpSession session = req.getSession();
							session.setAttribute("username",credentials.get("username"));
							res.setStatus(200);
							return;
						}
					}
				}
				res.getWriter().print("{ \"message\": \"Invalid Credentials\" }");
				res.setStatus(400);
			}
			if (portions[2].equals("register")) {
				if (JSONTools.receiveJSON(req, row)) {
					row.put("user_id",0);	// Necessary ?
					int user_id = users.create(row);
					if (0 < user_id) {
						JSONTools.dispenseJSON(res, users.readOne(user_id));
						res.setStatus(201);
						return;
					}
				}
				res.getWriter().print("{ \"message\": \"Invalid fields\" }");
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
