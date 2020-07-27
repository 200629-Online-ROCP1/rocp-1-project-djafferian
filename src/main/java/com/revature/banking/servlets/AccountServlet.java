package com.revature.banking.servlets;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
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
			Row row = account.getRow();
			JsonObject txn;
			int account_id;
			double amount, balance;
			switch (action[0]) {
			case "accounts":
				if (!JSONTools.convertJsonObjectToRow(reqBody, row)) {
					res.setStatus(res.SC_BAD_REQUEST);
					return;
				}
				account_id = account.create(row);
				if (0 == account_id) {
					res.setStatus(res.SC_INTERNAL_SERVER_ERROR);
					return;
				}
				AccountUsers au = new AccountUsers();
				row = au.getRow();
				row.get("user_id", Integer.valueOf(action[1]));
				row.get("account_id", account_id);
				au.create(row);
				JSONTools.dispenseJSON(res, account.readOne(account_id));
				res.setStatus(res.SC_CREATED);
				return;
			case "deposit":
				txn = reqBody.asObject();
				account_id = txn.getInt("accountId", 0);
				amount = txn.getDouble("amount", Double.NaN);
				if (amount <= 0 || Double.valueOf(amount).isNaN()) {
					res.setStatus(res.SC_BAD_REQUEST);
					return;
				}
				row = account.readOne(account_id);
				balance = ((Double)row.get("balance")).doubleValue();
				row.put("balance", Double.valueOf(balance + amount));
				account.update(row);
				JSONTools.dispenseJSONMessage(res, "$"+amount+
						" has been deposited to Account #"+account_id);
				break;
			case "withdraw":
				txn = reqBody.asObject();
				account_id = txn.getInt("accountId", 0);
				amount = txn.getDouble("amount", Double.NaN);
				row = account.readOne(account_id);
				balance = ((Double)row.get("balance")).doubleValue();
				if (amount <= 0 || balance < amount ||
						Double.valueOf(amount).isNaN()) {
					res.setStatus(res.SC_BAD_REQUEST);
					return;
				}
				row.put("balance", Double.valueOf(balance - amount));
				account.update(row);
				JSONTools.dispenseJSONMessage(res, "$"+amount+
						" has been withdrawn from Account #"+account_id);
				break;
			case "transfer" :
				txn = reqBody.asObject();
				account_id = txn.getInt("sourceAccountId", 0);
				amount = txn.getDouble("amount", Double.NaN);
				row = account.readOne(account_id);
				balance = ((Double)row.get("balance")).doubleValue();
				if (amount <= 0 || balance < amount ||
						Double.valueOf(amount).isNaN()) {
					res.setStatus(res.SC_BAD_REQUEST);
					return;
				}
				row.put("balance", Double.valueOf(balance - amount));
				account.update(row);

				account_id = txn.getInt("targetAccountId", 0);
				row = account.readOne(account_id);
				balance = ((Double)row.get("balance")).doubleValue();
				row.put("balance", Double.valueOf(balance + amount));
				account.update(row);

				JSONTools.dispenseJSONMessage(res, "$"+amount+
						" has been transferred from Account #"+
						txn.get("sourceAccountId")+" to Account #"+
						txn.get("targetAccountId"));
			}
			res.setStatus(res.SC_OK);
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
			Account account = new Account();
			Row row = account.getRow();
			if (!JSONTools.convertJsonObjectToRow(reqBody, row)) {
				res.setStatus(res.SC_BAD_REQUEST);
				return;
			}
			int account_id = ((Integer)row.get("account_id")).intValue();
			if (0 == account_id) {
				res.setStatus(res.SC_INTERNAL_SERVER_ERROR);
				return;
			}
			account.update(row);
			JSONTools.dispenseJSON(res, account.readOne(account_id));
		} catch (SQLException ex) {
			handleSQLException (ex, res);
		}
		res.setStatus(res.SC_OK);
	}
}
