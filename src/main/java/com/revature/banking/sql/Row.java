package com.revature.banking.sql;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/***
 * A Row is a fixed size and structurally immutable map of insertion ordered
 * entries, the keys of which are Strings.  The entry types can vary among the
 * entries but do not change per entry, although the values can.
 * 
 * @author Owner
 *
 */
public class Row extends LinkedHashMap<String,Object> {

	public Row() {}	// "Creator" constructor, to satisfy Jackson Databind ??
	public Row(Map<String,Object> map) { super(map); }	// copy constructor

	// Disinheritance
	private final void		clean() {}
	private final Object	merge​(String key, Object value,
            BiFunction<Object,Object,? extends Object> remappingFunction) { return null; }
	private final void		putAll​(Map<? extends Object,? extends Object> m) {}
	private final Object	remove(String key) { return null; }
	private final boolean	remove(String key, Object value) { return false; }
	private final void		replaceAll​(
			BiFunction<String,Object,? extends Object> function) {}

	// The Row.put() method does the same as the Row.replace() method;
	public Object put(String key, Object value) {
		return replace(key, value);
	}	
	// The Row.replace() methods will return null if the objects being
	// compared are of different class. If they are of the same class
	// then Row.replace() will do what Map.replace() does.
	public Object replace(String key, Object value) {
		Object o = this.get(key);
		if (!o.getClass().equals(value.getClass())) return null;
		return super.replace(key, value);
	}
	public boolean replace​(String key, Object oldValue, Object newValue) {
		return oldValue.getClass().equals(newValue.getClass()) &&
				super.replace(key, oldValue, newValue);
	}
}