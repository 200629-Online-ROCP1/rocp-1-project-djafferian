package com.revature.banking.servlets;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.revature.banking.sql.Account;
import com.revature.banking.sql.AccountUsers;
import com.revature.banking.sql.Roles;
import com.revature.banking.sql.Row;
import com.revature.banking.sql.Statuses;
import com.revature.banking.sql.Users;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AccountServlet extends HttpServlet {
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
			throws ServletException, IOException {
		// A GET request does not have request body.
		res.setContentType("application/json");
		res.setStatus(res.SC_NOT_FOUND);	// Presume failure
		try {
			String[] action = Permissions.granted(req, reqBody);
			if (action == null) { JSONTools.securityBreach(req, res); return; }
			Account account = new Account();
			Row row = account.getRow();
			if (!JSONTools.convertJsonObjectToRow(reqBody, row)) {
				res.setStatus(res.SC_BAD_REQUEST);
				return;
			}
			Object obj = null;
			switch (action[0]) {
			case "accounts":
				obj = 1 == action.length ? account.readAll() :
					account.readOne(Integer.parseUnsignedInt(action[1]));
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
	
	@SuppressWarnings("static-access")
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		reqBody = Json.parse((Reader)req.getReader());
		res.setContentType("application/json");
		res.setStatus(res.SC_NOT_FOUND);	// Presume failure
		try {
			String[] action = Permissions.granted(req, reqBody);
			if (action == null) { JSONTools.securityBreach(req, res); return; }
			Account account = new Account();
			if (portions[2].equals("accounts")) {
				Row row = account.getRow();
				if (JSONTools.receiveJSON(req, row)) {
					row.put("account_id",0);	// Necessary ?
					int account_id = account.create(row);
					if (0 < account_id) {
						JSONTools.dispenseJSON(res, account.readOne(account_id));
						res.setStatus(201);
						return;
					}
				}
			} else if (portions[2].equals("transfer")) {
				Map<String,Object> txn = new HashMap<String,Object>();
				txn.put("sourceAccountId", Integer.valueOf(0));
				txn.put("targetAccountId", Integer.valueOf(0));
				txn.put("amount", Double.valueOf(0));
				if (!JSONTools.receiveJSON(req, txn)) return;
				Double amount = (Double)txn.get("amount");
				if (amount <= 0) return;
				Row sourceRow = account.readOne((Integer)txn.get("sourceAccountId"));
				Row targetRow = account.readOne((Integer)txn.get("targetAccountId"));
				Double sourceBalance = (Double)sourceRow.get("balance");
				Double targetBalance = (Double)targetRow.get("balance");
				if (sourceBalance < amount) return;
				sourceRow.put("balance", sourceBalance-amount);
				targetRow.put("balance", targetBalance+amount);
				account.update(sourceRow);
				account.update(targetRow);
				JSONTools.dispenseJSONMessage(res, "$"+amount+
						" has been transferred to Account #"+
						txn.get("sourceAccountId")+" to Account #"+
						txn.get("targetAccountId"));
				res.setStatus(200);
			} else {
				Map<String,Object> txn = new HashMap<String,Object>();
				txn.put("accountId", Integer.valueOf(0));
				txn.put("amount", Double.valueOf(0));
				if (!JSONTools.receiveJSON(req, txn)) return;
				Double amount = (Double)txn.get("amount");
				if (amount <= 0) return;
				Row row = account.readOne((Integer)txn.get("accountId"));
				Double balance = (Double)row.get("balance");
				String verb;
				if (portions[2].equals("deposit")) {
					balance += amount;
					verb = "deposited to";
				} else if (portions[2].equals("withdraw")) {
					if (balance < amount) return;
					balance -= amount;
					verb = "withdrawn from";
				} else return;
				row.put("balance", balance);
				account.update(row);
				res.getWriter().print(
						"{\"message\": \"$"+amount+" has been "+
						verb+" Account #"+txn.get("accountId")+"\"}");
				res.setStatus(200);
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
		res.setStatus(400);	// Presume failure
		final String URI = req.getRequestURI();
		String[] portions = URI.split("/");
		Integer accountId = null;
		Integer userId = null;
		switch (portions.length) {
		case 3:
			if (portions[2].equals("accounts")) break;
		default:
			return;
		}
		
		try {
			Account account = new Account();
			Row row = account.getRow();
			if (!JSONTools.receiveJSON(req, row)) return;
			accountId = (Integer)row.get("account_id");
			AccountUsers account_users = new AccountUsers();
			
			HttpSession session = req.getSession(false);
			if (session == null) {
				JSONTools.securityBreach(req, res);
				return;
			}
			Roles role = (Roles)session.getAttribute("role");
			if (role !=  Roles.administrator &&
					!account_users.accountOwner(accountId,
							(Integer)session.getAttribute("user_id"))) {
				JSONTools.securityBreach(req, res);
				return;
			}
			
			account.update(row);
			res.setStatus(200);
		} catch (SQLException ex) {
			handleSQLException (ex, res);
		}
	}
}
