package com.revature.banking.servlets;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.revature.banking.sql.AccountUsers;
import com.revature.banking.sql.Roles;
import com.revature.banking.sql.Row;
import com.revature.banking.sql.Users;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class UsersServlet extends HelperServlet {

	private static String[] URIpatternStrings = {
			"^/(login)$",
			"^/(logout)$",
			"^/(register)$",
			"^/(users)$",
			"^/(users)/([1-9][0-9]*)$"
	};
	
	public UsersServlet () {
		super(URIpatternStrings);
	}
	
	public boolean authorized (HttpServletRequest req, String[] action)
			throws SQLException, IOException {

		HttpSession session = req.getSession(false);
		String method = req.getMethod();
		switch (action[0]) {
		case "logout":
			switch (method) {
			case "POST":
				return true;	// "logout" is always authorized.
			}
			break;
		// "login" is authorized when there is no session, while all other
		// actions are unauthorized without a session.  When there is a
		// session, "login" is authorized only when the credentials are
		// those for the session user identified in the session.
		case "login":
			switch (method) {
			case "POST":
				if (session == null) return true;
			}
		}
		if (session == null) return false;
		Row row = getSessionUser(req);
		switch (action[0]) {
		case "login":
			switch (method) {
			case "POST":
				// If the credentials match those of the session user,
				// do nothing.  Otherwise, invalidate the session.
				JsonObject credentials = getRequestBody(req).asObject();
				return credentials.get("username").asString()
						.equals(row.get("username"))
					&& credentials.get("password").asString()
						.equals(row.get("password"));
			}
		}
		
		Roles role = (Roles)row.get("role");
		if (role == null) return false;
		if (role == Roles.administrator) return true;
		if (role == Roles.employee) return method.equals("GET");
		
		int user_id = ((Integer)row.get("user_id")).intValue();
		switch (action[0]) {
		case "users":
			switch (method) {
			case "GET":
				return (action.length == 2 &&
						Integer.parseInt(action[1]) == user_id;
			case "PUT":
				JsonValue reqBody = getRequestBody(req);
				if (reqBody == null) return false;
				return reqBody.asObject().get("user_id").asInt() == user_id;
			}
		}
		return false;
	}

	@SuppressWarnings("static-access")
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws IOException, ServletException {

		res.setContentType("application/json");
		res.setStatus(res.SC_NOT_FOUND);	// Presume failure
		String[] action = getPathArguments(req);
		if (action == null) return;
		try {
			if (!authorized(req, action)) {
				JSONTools.securityBreach(req, res);
				return;
			}
			switch (action[0]) {
			case "users":
				Users users = new Users();
				JSONTools.dispenseJSON(res,
						action.length == 1 ? users.readAll() :
						users.readOne(Integer.parseUnsignedInt(action[1])));
				res.setStatus(res.SC_OK);
			}
	    } catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
	
	@SuppressWarnings("static-access")
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		resetRequestBody();
		res.setContentType("application/json");
		res.setStatus(res.SC_NOT_FOUND);	// Presume failure
		String[] action = getPathArguments(req);
		if (action == null) return;

		try {
			if (!authorized(req, action)) {
				JSONTools.securityBreach(req, res);
				return;
			}
			Users users = new Users();
			Row row = getSessionUser(req);
			switch (action[0]) {
			case "login":
				// At this point, the "authorized" method has already
				// determined that there is a session and the credentials
				// in the request body are those of the session user, OR
				// there is no session and whether the credentials belong
				// to any user is yet to be determined.
				if (req.getSession(false) != null) {
					JSONTools.dispenseJSON(res, row);
					res.setStatus(res.SC_OK);
					return;
				}

				users = new Users();
				JsonObject credentials = getRequestBody(req).asObject();
				ArrayList<Row> rows = users.readSome("username",
						credentials.get("username").asString());
				// The "username" column is constrained to be unique,
				// so there should only be one row returned.
				if (1 < rows.size()) {
					res.setStatus(res.SC_INTERNAL_SERVER_ERROR);
					return;
				}
				if (1 == rows.size()) {
					row = rows.get(0);
					if (credentials.get("password").asString().equals(row.get("password"))) {
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
					row = getSessionUser(req);
					session.invalidate();
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
				if (JSONTools.convertJsonObjectToRow(getRequestBody(req), row)) {
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
				return;
			}
		} catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
	
	@SuppressWarnings("static-access")
	protected void doPut(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		resetRequestBody();
		res.setContentType("application/json");
		res.setStatus(res.SC_NOT_FOUND);	// Presume failure
		String[] action = getPathArguments(req);
		if (action == null) return;
		
		try {
			if (!authorized(req, action)) {
				JSONTools.securityBreach(req, res);
				return;
			}	
			switch (action[0]) {
			case "users":
				Users users = new Users();
				Row row = users.getRow();
				if (JSONTools.convertJsonObjectToRow(getRequestBody(req), row)) {
					try {
						users.update(row);
					} catch (SQLException ex) {
						throw ex;
					} finally {
						int user_id = ((Integer)row.get("user_id")).intValue();
						row = users.readOne(user_id);
						JSONTools.dispenseJSON(res, row);
						res.setStatus(res.SC_OK);
					}
				}
				res.setStatus(res.SC_BAD_REQUEST);
			}
		} catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
}
