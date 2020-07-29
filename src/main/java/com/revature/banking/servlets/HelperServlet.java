package com.revature.banking.servlets;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.revature.banking.sql.Roles;
import com.revature.banking.sql.Row;
import com.revature.banking.sql.Users;

public abstract class HelperServlet extends HttpServlet {
	
	private Pattern[] URIpatterns;

	public HelperServlet(String[] URIpatternStrings) {
		URIpatterns = new Pattern[URIpatternStrings.length];
		for (int i=0; i<URIpatternStrings.length; i+=1)
			URIpatterns[i] = Pattern.compile(URIpatternStrings[i]);
	}
	
	protected Pattern[] getURIpatterns() { return URIpatterns; }
	
	public String[] getPathArguments (HttpServletRequest req) {
		String sp = req.getServletPath();
		String pi = req.getPathInfo();
		if (pi != null) sp += pi;
		String[] action = null;
		for (Pattern pattern : getURIpatterns()) {
			Matcher matcher = pattern.matcher(sp);
			if (matcher.find()) {
				int n = matcher.groupCount();
				action = new String[n];
				// matcher.group(0) is the string which matches
				// the entire pattern; we don't want that.
				for (int i=0; i<n; i+=1) action[i] = matcher.group(i+1);
				break;
			}
		}
		return action;
	}
	
	// This method gets the row of the "users" table for the session user.
	protected Row getSessionUser (HttpServletRequest req) throws SQLException {
		HttpSession session = req.getSession(false);
		Integer userId = (Integer)session.getAttribute("user_id");
		int user_id = userId.intValue();
		Row row;
		try {
			Users users = new Users();
			row = users.readOne(user_id);
		} catch (SQLException ex) {
			throw ex;
		}
		return row;
	}

	// Since null is a valid JSON value, a flag is needed to record
	// that reading the request body has already been attempted.
	private byte haveReqBody = 0;
	private JsonValue reqBody;
	// The resetRequestBody method must be called at the start of
	// any doXxx method for which a request body may be expected.
	protected void resetRequestBody() { haveReqBody = 0; }

	// This method should not be called unless the request body is expected to
	// be valid JSON and the information in it is needed to process the request.
	protected JsonValue getRequestBody(HttpServletRequest req) throws IOException {
		if (haveReqBody == 1) return reqBody;
		if (haveReqBody == -1) return null;
		try {
			reqBody = Json.parse((Reader)req.getReader());
			haveReqBody = 1;
			return reqBody;
		} catch (ParseException ex) {
			System.err.println(ex.getMessage());
			haveReqBody = -1;
			return null;
		}
	}
	
	@SuppressWarnings("static-access")
	protected void handleSQLException (SQLException ex, HttpServletResponse res) {
    	System.err.println("SQLException: " + ex.getMessage());
    	System.err.println("SQLState: " + ex.getSQLState());
    	System.err.println("VendorError: " + ex.getErrorCode());
		ex.printStackTrace();
		res.setStatus(res.SC_INTERNAL_SERVER_ERROR);
	}

}
