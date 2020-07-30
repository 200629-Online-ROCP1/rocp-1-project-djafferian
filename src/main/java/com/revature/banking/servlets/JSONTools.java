package com.revature.banking.servlets;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.banking.sql.Roles;
import com.revature.banking.sql.Row;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class JSONTools {

	private JSONTools() {}

	// This is the only use of Jackson Databind, until
	// I can put com.eclipsesource.json classes to the task.
	public static final ObjectMapper om = new ObjectMapper();
	public static void dispenseJSON (HttpServletResponse res, Object o)
			throws IOException, JsonProcessingException {
		res.getWriter().print(om.writeValueAsString(o));
	}
	
	public static void dispenseJSONMessage(HttpServletResponse res, String message)
			throws IOException {
		JsonObject jo = new JsonObject();
		jo.set("message", message).writeTo(res.getWriter());
	}
	public static void securityBreach(HttpServletRequest req, HttpServletResponse res)
			throws IOException {
		JSONTools.dispenseJSONMessage(res, "The requested action is not permitted.");
		res.setStatus(res.SC_UNAUTHORIZED);
	}
	
	/**
	 * The members of JSON objects are not ordered, while the Row class used
	 * with databases are ordered.  This method ensures that the JsonValues
	 * from the former are converted to Java Objects in the latter, while
	 * maintaining order and inclusion.
	 * 
	 * @param jo
	 * @param row
	 * @return true if the conversion completes, false if the conversion fails.
	 */
	public static boolean convertJsonObjectToRow (JsonValue jv, Row row) {
		if (!jv.isObject()) return false;
		JsonObject jo = jv.asObject();
		if (jo.size() != row.size()) return false;
		for (Member field : jo) {
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
				for (Object e : o.getClass().getEnumConstants())
					if (((Enum)e).name().equals(value.asString())) {
						row.put(name, e);
						break;
					}
				continue;
			} else return false;
		}
		return true;
	}
}
