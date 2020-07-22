package com.revature.banking.servlets;

import com.revature.banking.sql.Account;
import com.revature.banking.sql.Roles;
import com.revature.banking.sql.Row;

import java.io.IOException;
import java.io.Reader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AccountServlet extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		res.setContentType("application/json");
		res.setStatus(404);	// Presume failure
		final String URI = req.getRequestURI();
		String[] portions = URI.split("/");
		Integer accountId = null;
		switch (portions.length) {
		case 4:
			try {
				accountId = Integer.valueOf(portions[3]);
			} catch (NumberFormatException nfe) {
				return;
			}
		case 3:
			if (portions[2].equals("accounts")) break;
		default:
			return;
		}
		
		try {
			Account account = new Account();
			JSONTools.dispenseJSON(res,
					accountId == null ? account.readAll() :
					account.readOne(accountId.intValue()));
			res.setStatus(200);
		} catch (Exception ex) {
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
			if (portions[2].equals("account")) break;
		default:
			return;
		}
		
		try {
			Account account = new Account();
			Row row = account.getRow();
			if (!JSONTools.receiveJSON(req, row)) return;
			account.update(row);
			res.setStatus(200);
		} catch (Exception ex) {
			ex.printStackTrace();
			res.setStatus(501);
			return;
		}
	}
}
