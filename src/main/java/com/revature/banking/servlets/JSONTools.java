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
		req.getSession().invalidate();
		JSONTools.dispenseJSONMessage(res, "The requested action is not permitted");
		res.setStatus(401);
	}
	
	public static boolean receiveJSON (HttpServletRequest req, Map<String,Object> map)
			throws IOException {
		JsonValue jsonRow = Json.parse((Reader)req.getReader());
		if (!jsonRow.isObject()) return false;
		JsonObject jsonFields = jsonRow.asObject();
		if (jsonFields.size() != map.size()) return false;
		for (Member field : jsonFields) {
			String name = field.getName();
			JsonValue value = field.getValue();
			Object o = map.get(name);
			if (o == null || value.isNull()) return false;
			if (o instanceof String) {
				if (!value.isString()) return false;
				map.put(name, value.asString());
				continue;
			} else if (o instanceof Integer) {
				if (!value.isNumber()) return false;
				map.put(name, value.asInt());
				continue;
			} else if (o instanceof Long) {
				if (!value.isNumber()) return false;
				map.put(name, value.asLong());
				continue;
			} else if (o instanceof Float) {
				if (!value.isNumber()) return false;
				map.put(name, value.asFloat());
				continue;
			} else if (o instanceof Double) {
				if (!value.isNumber()) return false;
				map.put(name, value.asDouble());
				continue;
			} else if (o instanceof Boolean) {
				if (!value.isBoolean()) return false;
				map.put(name, value.asBoolean());
				continue;
			} else if (o instanceof Enum) {
				if (!value.isString()) return false;
				map.put(name, value.asString());
				continue;
			} else return false;
		}
		return true;
	}
}
