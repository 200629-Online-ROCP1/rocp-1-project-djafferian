package com.revature.banking.servlets;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.revature.banking.sql.Account;
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

public class AccountServlet extends HelperServlet {

	private static String[] GET_URIpatternStrings = {
			"^/(accounts)$",
			"^/(accounts/owner)/([1-9][0-9]*)$",
			"^/(accounts/status)/([^/]+)$",
			"^/(accounts)/([1-9][0-9]*)$"
	};
	private static String[] POST_URIpatternStrings = {
			"^/(accounts)$",
			"^/(accounts/deposit)$",
			"^/(accounts/transfer)$",
			"^/(accounts/withdraw)$"
	};
	private static String[] PUT_URIpatternStrings = { "^/(accounts)$" };
	
	public AccountServlet () {
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
		int user_id = ((Integer)row.get("user_id")).intValue();
		Account account = new Account();
		switch (action[0]) {
		case "accounts":
			if (action.length == 1) return false;
			row = account.readOne(Integer.parseInt(action[1]));
			return ((Integer)row.get("user_id")).intValue() == user_id;
		case "accounts/owner":
			return Integer.parseInt(action[1]) == user_id;
		case "accounts/status":
			return false;
		}
		return false;
	}

	@SuppressWarnings("static-access")
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		res.setContentType("application/json");
		res.setStatus(res.SC_NOT_FOUND);	// Presume failure
		String[] action = getPathArguments(req);
		if (action == null) return;
		try {
			if (!doGetAuthorized(req, action)) {
				JSONTools.securityBreach(req, res);
				return;
			}
			Account account = new Account();
			Object obj = null;	// Could be Row, or ArrayList<Row>
			switch (action[0]) {
			case "accounts":
				obj = 1 == action.length ? account.readAll() :
					account.readOne(Integer.parseInt(action[1]));
				break;
			case "accounts/owner":
				obj = account.readSome("user_id", Integer.parseInt(action[1]));
				break;
			case "accounts/status":
				obj = account.readSome("status", action[1]);
				break;
			}
			JSONTools.dispenseJSON(res, obj);
			res.setStatus(res.SC_OK);
		} catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
	
	private boolean doPostAuthorized (HttpServletRequest req, String[] action)
			throws SQLException, IOException {
		HttpSession session = req.getSession(false);
		if (session == null) return false;
		Row row = getSessionUser(session);
		if (row == null) return false;
		Roles role = (Roles)row.get("role");
		if (role == Roles.administrator) return true;
		JsonObject reqBody = getRequestBody(req).asObject();		
		if (reqBody == null) return false;
		int user_id = ((Integer)row.get("user_id")).intValue();
		int account_id = 0;
		switch (action[0]) {
		case "accounts":
			if (role == Roles.employee) return true;
			return reqBody.get("user_id").asInt() == user_id;
		case "accounts/deposit":
		case "accounts/withdraw":
			account_id = reqBody.get("accountId").asInt();
			break;
		case "accounts/transfer":
			account_id = reqBody.get("sourceAccountId").asInt();
			break;
		}
		if (account_id == 0) return false;
		Account account = new Account();
		row = account.readOne(account_id);
		return ((Integer)row.get("user_id")).intValue() == user_id;
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
			Account account = new Account();
			Row row = account.getRow();
			JsonObject txn;
			int account_id;
			double amount, balance;
			switch (action[0]) {
			case "accounts":
				if (!JSONTools.convertJsonObjectToRow(getRequestBody(req), row)) {
					res.setStatus(res.SC_BAD_REQUEST);
					return;
				}
				account_id = account.create(row);
				if (0 == account_id) {
					res.setStatus(res.SC_INTERNAL_SERVER_ERROR);
					return;
				}
				JSONTools.dispenseJSON(res, account.readOne(account_id));
				res.setStatus(res.SC_CREATED);
				return;
			case "accounts/deposit":
				txn = getRequestBody(req).asObject();
				account_id = txn.getInt("accountId", 0);
				amount = txn.getDouble("amount", Double.NaN);
				if (txn.size() != 2 || amount <= 0 ||
						Double.valueOf(amount).isNaN()) {
					res.setStatus(res.SC_BAD_REQUEST);
					return;
				}
				row = account.readOne(account_id);
				balance = ((Double)row.get("balance")).doubleValue();
				row.put("balance", Double.valueOf(balance + amount));
				account.update(row);
				JSONTools.dispenseJSONMessage(res, "$"+amount+
						" has been deposited to Account #"+account_id);
				res.setStatus(res.SC_OK);
				return;
			case "accounts/withdraw":
				txn = getRequestBody(req).asObject();
				account_id = txn.getInt("accountId", 0);
				amount = txn.getDouble("amount", Double.NaN);
				row = account.readOne(account_id);
				balance = ((Double)row.get("balance")).doubleValue();
				if (txn.size() != 2 || amount <= 0 || balance < amount ||
						Double.valueOf(amount).isNaN()) {
					JSONTools.dispenseJSONMessage(res, "$"+amount+
							" could not be withdrawn from Account #"+account_id);
					res.setStatus(res.SC_BAD_REQUEST);
					return;
				}
				row.put("balance", Double.valueOf(balance - amount));
				account.update(row);
				JSONTools.dispenseJSONMessage(res, "$"+amount+
						" has been withdrawn from Account #"+account_id);
				res.setStatus(res.SC_OK);
				return;
			case "accounts/transfer" :
				txn = getRequestBody(req).asObject();
				int sourceAccountId = txn.getInt("sourceAccountId", 0);
				int targetAccountId = txn.getInt("targetAccountId", 0);
				amount = txn.getDouble("amount", Double.NaN);
				row = account.readOne(sourceAccountId);
				balance = ((Double)row.get("balance")).doubleValue();
				if (txn.size() != 3 || amount <= 0 || balance < amount ||
						Double.valueOf(amount).isNaN()) {
					JSONTools.dispenseJSONMessage(res, "$"+amount+
							" could not be withdrawn from Account #"+sourceAccountId);
					res.setStatus(res.SC_BAD_REQUEST);
					return;
				}
				row.put("balance", Double.valueOf(balance - amount));
				account.update(row);

				row = account.readOne(targetAccountId);
				balance = ((Double)row.get("balance")).doubleValue();
				row.put("balance", Double.valueOf(balance + amount));
				account.update(row);

				JSONTools.dispenseJSONMessage(res, "$"+amount+
						" has been transferred from Account #"+sourceAccountId+
						" to Account #"+targetAccountId);
				res.setStatus(res.SC_OK);
				return;
			}
		} catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
	
	private boolean doPutAuthorized (HttpServletRequest req, String[] action)
			throws SQLException {
		HttpSession session = req.getSession(false);
		if (session == null) return false;
		Row row = getSessionUser(session);
		if (row == null) return false;
		Roles role = (Roles)row.get("role");
		// Only an admin can update an account.
		if (role == Roles.administrator) return true;
		switch (action[0]) {
		case "accounts":
			return false;
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
			Account account = new Account();
			Row row = account.getRow();
			if (!JSONTools.convertJsonObjectToRow(getRequestBody(req), row)) {
				res.setStatus(res.SC_BAD_REQUEST);
				return;
			}
			account.update(row);
			JSONTools.dispenseJSON(res, account.readOne(
					((Integer)row.get("account_id")).intValue()));
			res.setStatus(res.SC_OK);
		} catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
}
