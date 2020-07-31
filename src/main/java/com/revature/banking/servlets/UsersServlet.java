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
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class UsersServlet extends HelperServlet {

	private static String[] GET_URIpatternStrings = {
			"^/(users)$", "^/(users)/([1-9][0-9]*)$" };
	private static String[] POST_URIpatternStrings = {
			"^/(login)$", "^/(logout)$", "^/(register)$" };
	private static String[] PUT_URIpatternStrings = { "^/(users)$" };
	
	public UsersServlet () {
		super(GET_URIpatternStrings, POST_URIpatternStrings, PUT_URIpatternStrings);
	}

	private boolean doGetAuthorized (HttpServletRequest req, String[] action)
			throws SQLException {
		HttpSession session = req.getSession(false);
		if (session == null) return false;
		Row row = getSessionUser(session);
		if (row == null) return false;
		Roles role = (Roles)row.get("role");
		if (role == Roles.administrator) return true;
		if (role == Roles.employee) return true;	// because this is a GET
		switch (action[0]) {
		case "users":
			int user_id = ((Integer)row.get("user_id")).intValue();
			return action.length == 2 &&
					Integer.parseInt(action[1]) == user_id;
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
			if (!doGetAuthorized(req, action)) {
				JSONTools.securityBreach(req, res);
				return;
			}
			switch (action[0]) {
			case "users":
				Users users = new Users();
				JSONTools.dispenseJSON(res, action.length == 2
						? users.readOne(Integer.parseInt(action[1]))
						: users.readAll());
				res.setStatus(res.SC_OK);
			}
	    } catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
	
	private boolean doPostAuthorized (HttpServletRequest req, String[] action)
			throws SQLException, IOException {
		HttpSession session = req.getSession(false);
		Row row;
		
		switch (action[0]) {
		// "login" is authorized when there is no session.  "login" is also
		// authorized when there is a session and when the credentials are
		// those for the session user.
		case "login":
			if (session == null) return true;
			row = getSessionUser(session);
			if (row == null) return false;
			JsonObject creds = getRequestBody(req).asObject();
			return creds.get("username").asString().equals(row.get("username"))
				&& creds.get("password").asString().equals(row.get("password"));
		case "logout":
			return true;	// "logout" is always authorized.
		case "register":
			if (session == null) return false;
			row = getSessionUser(session);
			if (row == null) return false;
			Roles role = (Roles)row.get("role");
			if (role == Roles.administrator) return true;
		}
		return false;
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
			if (!doPostAuthorized(req, action)) {
				JSONTools.securityBreach(req, res);
				return;
			}
			HttpSession session = req.getSession(false);
			Users users = new Users();
			Row row;
			switch (action[0]) {
			case "login":
				// At this point, either there is a session and the credentials
				// in the request body are those of the session user, OR there
				// is no session and whether the credentials belong to any user
				// is yet to be determined.
				if (session != null) {
					row = getSessionUser(session);
					JSONTools.dispenseJSON(res, row);
					res.setStatus(res.SC_OK);
					return;
				}

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
					if (credentials.get("password").asString()
							.equals(row.get("password"))) {
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
				if (session != null) {
					row = getSessionUser(session);
					session.invalidate();
					JSONTools.dispenseJSONMessage(res,
							"You have successfully logged out "+row.get("username"));
					res.setStatus(res.SC_OK);
				} else {
					JSONTools.dispenseJSONMessage(res,
							"There was no user logged into the session");
					res.setStatus(res.SC_BAD_REQUEST);				
				}
				return;
			case "register":
				row = users.getRow();
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

	private boolean doPutAuthorized (HttpServletRequest req, String[] action)
			throws SQLException, IOException {
		HttpSession session = req.getSession(false);
		if (session == null) return false;
		Row row = getSessionUser(session);
		if (row == null) return false;
		switch (action[0]) {
		case "users":
			Roles role = (Roles)row.get("role");
			if (role == Roles.administrator) return true;
			JsonValue reqBody = getRequestBody(req);
			return reqBody.asObject().get("user_id").asInt() ==
					((Integer)row.get("user_id")).intValue();
		}
		return false;
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
			if (!doPutAuthorized(req, action)) {
				JSONTools.securityBreach(req, res);
				return;
			}
			res.setStatus(res.SC_BAD_REQUEST);
			switch (action[0]) {
			case "users":
				Users users = new Users();
				Row row = users.getRow();
				if (!JSONTools.convertJsonObjectToRow(
						getRequestBody(req), row)) return;
				try {
					users.update(row);
					res.setStatus(res.SC_OK);
				} catch (SQLException ex) {
					handleSQLException (ex, res);
				}
				row = users.readOne(((Integer)row.get("user_id")).intValue());
				JSONTools.dispenseJSON(res, row);
			}
		} catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
}
