package com.revature.banking.servlets;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
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
	JsonValue reqBody;	// Not used for requests with no body.
	
	@SuppressWarnings("static-access")
	private void handleSQLException (SQLException ex, HttpServletResponse res) {
    	System.err.println("SQLException: " + ex.getMessage());
    	System.err.println("SQLState: " + ex.getSQLState());
    	System.err.println("VendorError: " + ex.getErrorCode());
		ex.printStackTrace();
		res.setStatus(res.SC_INTERNAL_SERVER_ERROR);
	}

	@SuppressWarnings("static-access")
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws IOException, ServletException {
		// A GET request does not have request body.
		res.setContentType("application/json");
		res.setStatus(res.SC_NOT_FOUND);	// Presume failure
		
		try {
			String[] action = Permissions.granted(req, null);
			if (action == null) { JSONTools.securityBreach(req, res); return; }
			Users users = new Users();
			JSONTools.dispenseJSON(res,
					action.length == 1 ? users.readAll() :
					users.readOne(Integer.parseUnsignedInt(action[1])));
			res.setStatus(res.SC_OK);
	    } catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
	
	@SuppressWarnings("static-access")
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		reqBody = Json.parse((Reader)req.getReader());
		res.setContentType("application/json");
		res.setStatus(res.SC_NOT_FOUND);	// Presume failure
		try {
			String[] action = Permissions.granted(req, reqBody);
			if (action == null) { JSONTools.securityBreach(req, res); return; }
			Users users = new Users();
			Row row = users.getRow();
			switch (action[0]) {
			case "login":
				JsonObject credentials = reqBody.asObject();
				ArrayList<Row> rows = users.readSome("username",
						credentials.get("username"));
				// The "username" column is supposed to have the unique
				// constraint, so there should only be one row returned.
				if (1 < rows.size()) {
					res.setStatus(res.SC_INTERNAL_SERVER_ERROR);
					return;
				}
				if (1 == rows.size()) {
					row = rows.get(0);
					if (credentials.get("password").equals(row.get("password"))) {
						req.getSession().setAttribute("user_id", row.get("user_id"));
						JSONTools.dispenseJSON(res, row);
						res.setStatus(res.SC_OK);
						return;
					}
				}
				JSONTools.dispenseJSONMessage(res, "Invalid Credentials");
				res.setStatus(res.SC_BAD_REQUEST);
				return;
			case "logout":
				HttpSession session = req.getSession(false);
				if (session != null) {
					Integer userId = (Integer)session.getAttribute("user_id");
					session.invalidate();
					row = users.readOne(userId.intValue());
					JSONTools.dispenseJSONMessage(res,
							"You have successfully logged out "+row.get("username"));
					res.setStatus(res.SC_OK);
				} else {
					JSONTools.dispenseJSONMessage(res,
							"There was no user logged into the session");
					res.setStatus(res.SC_BAD_REQUEST);				
				}
				break;
			case "register":
				if (JSONTools.convertJsonObjectToRow(reqBody, row)) {
					row.put("user_id",0);	// Necessary ?
					int user_id = users.create(row);
					if (0 < user_id) {
						JSONTools.dispenseJSON(res, users.readOne(user_id));
						res.setStatus(res.SC_CREATED);
						return;
					}
				}
				JSONTools.dispenseJSONMessage(res, "Invalid fields");
				res.setStatus(res.SC_BAD_REQUEST);
				break;
			}
		} catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
	
	@SuppressWarnings("static-access")
	protected void doPut(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		reqBody = Json.parse((Reader)req.getReader());
		res.setContentType("application/json");
		res.setStatus(res.SC_NOT_FOUND);	// Presume failure
				
		try {
			String[] action = Permissions.granted(req, reqBody);
			if (action == null) { JSONTools.securityBreach(req, res); return; }
			Users users = new Users();
			Row row = users.getRow();
			if (JSONTools.convertJsonObjectToRow(reqBody, row)) {
				users.update(row);
				res.setStatus(res.SC_OK);
				return;
			}
			res.setStatus(res.SC_BAD_REQUEST);
		} catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
}
