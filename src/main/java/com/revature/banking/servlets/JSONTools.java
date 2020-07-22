package com.revature.banking.servlets;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.banking.sql.Roles;
import com.revature.banking.sql.Row;

import java.io.Reader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class JSONTools {

	private JSONTools() {}

	// This is the only use for Jackson Databind, so be rid of it asap.
	public static final ObjectMapper om = new ObjectMapper();
	public static void dispenseJSON (HttpServletResponse res, Object o) {
		res.getWriter().print(om.writeValueAsString(o));
	}
	public static boolean receiveJSON (HttpServletRequest req, Row row) {
		JsonValue jsonRow = Json.parse((Reader)req.getReader());
		if (!jsonRow.isObject()) return false;
		JsonObject jsonFields = jsonRow.asObject();
		if (jsonFields.size() != row.size()) return false;
		for (Member field : jsonFields) {
			String name = field.getName();
			JsonValue value = field.getValue();
			Object o = row.get(name);
			if (o == null || value.isNull()) return false;
			if (o instanceof String) {
				if (!value.isString()) return false;
				row.put(name, value.asString());
				continue;
			} else if (o instanceof Integer) {
				if (!value.isNumber()) return false;
				row.put(name, value.asInt());
				continue;
			} else if (o instanceof Long) {
				if (!value.isNumber()) return false;
				row.put(name, value.asLong());
				continue;
			} else if (o instanceof Float) {
				if (!value.isNumber()) return false;
				row.put(name, value.asFloat());
				continue;
			} else if (o instanceof Double) {
				if (!value.isNumber()) return false;
				row.put(name, value.asDouble());
				continue;
			} else if (o instanceof Boolean) {
				if (!value.isBoolean()) return false;
				row.put(name, value.asBoolean());
				continue;
			} else if (o instanceof Enum) {
				if (!value.isString()) return false;
				row.put(name, value.asString());
				continue;
			} else return false;
		}
		return true;
	}
}
