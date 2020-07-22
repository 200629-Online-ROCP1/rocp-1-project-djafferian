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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UsersServlet extends HttpServlet {
	public static final ObjectMapper om = new ObjectMapper();

	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
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
			ex.printStackTrace();
			res.setStatus(501);
			return;
		}
	}
}
