package com.revature.banking.sql;

import java.util.LinkedHashMap;
import java.util.Set;

/***
 * A Row is a fixed size and structurally immutable map of insertion ordered
 * entries, the keys of which are Strings.  The entry types can vary among the
 * entries but do not change per entry, although the values can.
 * 
 * @author Owner
 *
 */
public class Row {
	LinkedHashMap<String,Object> lhm;
	Set<String> keyset;

	public Row(LinkedHashMap<String,Object> lhm) {
		this.lhm = lhm;
		this.keyset = lhm.keySet();
	}

	public Object get(String key) { return lhm.get(key); }
	public Set<String> keySet() { return keyset; }
	public int size() { return lhm.size(); }
	public Object put(String key, Object value) {
		Object o = lhm.get(key);
		if (o == null) return null;
		return lhm.put(key, value);
	}
}
