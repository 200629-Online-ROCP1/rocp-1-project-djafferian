package com.revature.banking.servlets;

import com.revature.banking.sql.Account;
import com.revature.banking.sql.Row;
import com.revature.banking.sql.Statuses;
import com.revature.banking.sql.Users;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AccountServlet extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		res.setContentType("application/json");
		res.setStatus(404);	// Presume failure
		final String URI = req.getRequestURI();
		String[] portions = URI.split("/");
		Integer accountId = null;
		Statuses status = null;
		switch (portions.length) {
		case 5:
			status = Statuses.valueOf(portions[4]);
		case 4:
			if (!portions[3].equals("status")) {
				try {
					accountId = Integer.valueOf(portions[3]);
				} catch (NumberFormatException nfe) {
					return;
				}
			}
		case 3:
			if (portions[2].equals("accounts")) break;
		default:
			return;
		}
		
		try {
			Account account = new Account();
			JSONTools.dispenseJSON(res, accountId != null
					? account.readOne(accountId.intValue())
					: status != null ? account.readSome("status",status)
									 : account.readAll());
			res.setStatus(200);
		} catch (SQLException ex) {
	    	// handle any exception
	    	System.err.println("SQLException: " + ex.getMessage());
	    	System.err.println("SQLState: " + ex.getSQLState());
	    	System.err.println("VendorError: " + ex.getErrorCode());
			ex.printStackTrace();
			res.setStatus(500);
			return;
		}
	}
	
	protected void doPut(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		res.setContentType("application/json");
		res.setStatus(400);	// Presume failure
		final String URI = req.getRequestURI();
		String[] portions = URI.split("/");
		Integer accountId = null;
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
			account.update(row);
			res.setStatus(200);
		} catch (SQLException ex) {
	    	// handle any exception
	    	System.err.println("SQLException: " + ex.getMessage());
	    	System.err.println("SQLState: " + ex.getSQLState());
	    	System.err.println("VendorError: " + ex.getErrorCode());
			ex.printStackTrace();
			res.setStatus(500);
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
		try {
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
				res.getWriter().print("{\"message\": \"$"+amount+
						" has been transferred to Account #"+
						txn.get("sourceAccountId")+" to Account #"+"\"}");
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
	    	// handle any exception
	    	System.err.println("SQLException: " + ex.getMessage());
	    	System.err.println("SQLState: " + ex.getSQLState());
	    	System.err.println("VendorError: " + ex.getErrorCode());
			ex.printStackTrace();
			res.setStatus(500);
		}
	}
}
