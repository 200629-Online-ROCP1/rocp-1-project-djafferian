package com.revature.banking.servlets;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.revature.banking.sql.AccountUsers;
import com.revature.banking.sql.Roles;
import com.revature.banking.sql.Row;
import com.revature.banking.sql.Users;

public class Permissions {
	// These patterns should be distinct; if not then the first matched
	// will be used.  If none match then a ServletException will be thrown.
	// Note that the '$' ensures that the match extends up to the end of
	// the URI.
	private static String[] URIpatternStrings = {
			"/(accounts)$",
			"/(accounts/deposit)$",
			"/(accounts/owner)/([1-9][0-9]*)$",
			"/(accounts/status)/([^/]+)$",
			"/(accounts/transfer)$",
			"/(accounts/withdraw)$",
			"/(accounts)/([1-9][0-9]*)$",
			"/(login)$",
			"/(logout)$",
			"/(register)$",
			"/(users)$",
			"/(users)/([1-9][0-9]*)$"
	};
	private static Pattern[] URIpatterns;

	// Singleton
	private Permissions() {
		URIpatterns = new Pattern[URIpatternStrings.length];
		for (int i=0; i<URIpatternStrings.length; i+=1)
			URIpatterns[i] = Pattern.compile(URIpatternStrings[i]);
	}
	private static Permissions instance = new Permissions();
	public static Permissions getInstance() { return instance; }
	
	public static String[] granted (HttpServletRequest req)
			throws SQLException, ServletException { return granted(req, null); }
	public static String[] granted (HttpServletRequest req, String idString)
			throws SQLException, ServletException {
		String sp = req.getServletPath();
		String[] groups = null;
		for (Pattern pattern : URIpatterns) {
			Matcher matcher = pattern.matcher(sp);
			if (matcher.find()) {
				int n = matcher.groupCount();
				groups = new String[n];
				// matcher.group(0) is the string which matches
				// the entire pattern; we don't want that.
				for (int i=0; i<n; i+=1) groups[i] = matcher.group(i+1);
			}
		}
		if (groups == null)
			throw new ServletException("No patterns match the servlet path '"+sp+"'.");
		HttpSession session = req.getSession(false);
		// Before any action a session must already exist,
		// except "login" for which there must be no session.
		if (groups[0].equals("login")) {
			if (session != null) groups[0] = "logout";
			return groups;
		}
		if (session == null) return null;
		Integer userId = (Integer)session.getAttribute("user_id");
		int user_id = userId.intValue();
		Users users = new Users();
		Row row = users.readOne(user_id);
		if (row == null) return null;

		Roles role = (Roles)row.get("role");		
		if (role == Roles.administrator) return groups;
		String method = req.getMethod();
		if (role == Roles.employee) switch (method) {
		case "GET":
			return groups;
		case "POST":
			if (groups[0].equals("accounts")) return groups;
		}
		// if the account user is the user logged in
		AccountUsers au = new AccountUsers();
		switch (method) {
		case "GET":
			switch (groups[0]) {
			case "users":
			case "accounts/owner":
				if (Integer.valueOf(groups[1]) == userId) return groups;
				break;
			case "accounts":
				if (au.accountOwner(Integer.valueOf(groups[1]), user_id)) return groups;
				break;
			}
			break;
		case "POST":
			switch (groups[0]) {
			case "accounts/deposit":
			case "accounts/transfer":
			case "accounts/withdraw":
				if (au.accountOwner(Integer.valueOf(idString), user_id)) return groups;
				break;
			case "logout":
				return groups;
			}
			break;
		case "PUT":
			switch (groups[0]) {
			case "users":
				if (Integer.valueOf(idString) == userId) return groups;
			}
		
		
		
				(method == "GET" ||
				(method == "POST" && groups[0] == "accounts"))) return groups;
		if (method == "GET" && (groups[0] == "users" || 
								groups[0] == "accounts/owner")) &&
			groups.length == 2 && Integer.valueOf(groups[1]) == userId) &&
		case "GET":
			switch (groups[0]) {
			case "users":
			case "accounts/owner":
				return groups;
			}
			break;
		case "POST":
			switch (groups[0]) {
			case "accounts":
				return groups;
			}
			break;
		case "PUT":
			case "users":
				if (Integer.valueOf(groups[1]) == userId) return groups;
			case "account":
				AccountUsers au = new AccountUsers();
				if (au.accountOwner(account_id, user_id)) switch (method) {
			}
			break;
		case "POST":
			switch (groups[0]) {
			case "accounts":
			case "logout":
				return groups;
			}
		}
		if (groups.length < 2) return null;
		
		if (Integer.valueOf(groups[1]) == userId) switch(method) {
		case "GET":
			switch (groups[0]) {
			case "accounts/owner":
			case "users":
				return groups;
			}
			break;
		case "POST":
			switch (groups[0]) {
			case "logout":
			case "users":
				return groups;
			}
			break;
		}
		AccountUsers au = new AccountUsers();
		if (au.accountOwner(account_id, user_id)) switch (method) {
		case "GET":
			switch (groups[0]) {
			case "accounts":
				return groups;
			}
			break;
		case "POST":
			switch (groups[0]) {
			case "accounts/deposit":
			case "accounts/transfer":
			case "accounts/withdraw":
				return groups;
			}
			break;
		case "PUT":
			switch(groups[0]) {
			case "users":
				return groups;
			}
			break;
		}
		return null;
	}
}
