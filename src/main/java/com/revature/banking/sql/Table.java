package com.revature.banking.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class requires that the table has a primary key composed
 * of a single column populated with auto-generated default values.
 * 
 * @author Owner
 *
 */
public abstract class Table implements DBContext {
	public Connection getConnection() {
		return DBConnectionManager.getConnection();
	}
	
	private static final String sqlSelectColumnCount =
			"SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ?";
	private static final String sqlSelectColumnNames =
			"SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ?";
	private static String packageName = Table.class.getPackageName();
	
	private String tableName;
	private String primaryKey;
	private Row row;
	
	public String getTableName () { return tableName; }
	public String getPrimaryKey () { return primaryKey; }
	public Row getRow() { return row; }

	public Table(String tn, String pk) throws Exception {
		tableName = tn;
		primaryKey = pk;
		/**
		 * Find out how many columns are in this table.
		 */
		PreparedStatement ps = getConnection().prepareStatement(sqlSelectColumnCount);
		ps.setString(1, tableName);
		ResultSet rs = ps.executeQuery();
		rs.next();
		int n = Integer.valueOf(rs.getString(1));
		/**
		 * Retrieve the name and data type for each column.
		 */
		LinkedHashMap<String,Object> row = new LinkedHashMap<String,Object>(n-1);
		ps = getConnection().prepareStatement(sqlSelectColumnNames);
		ps.setString(1, tableName);
		rs = ps.executeQuery();
		while (rs.next()) {
			String columnName = rs.getString(1);
			if (columnName.equals(primaryKey)) continue;
			String dataType = rs.getString(2);
			Object o = null;
			switch (dataType) {
			case "USER-DEFINED" :
				break;
			case "integer" :
				o = Integer.valueOf(0);
				break;
			case "character varying" :
				o = new String();
				break;
			}
			row.put(columnName, o);
		}
		this.row = new Row(row);
	}
	
	public int insert (Row row) throws Exception {
		/**
		 * First, make sure that the row passed here came from here.
		 */
		Exception ex = new Exception(
				"The Row argument did not originate from the getRow method.");
		if (this.row.size() != row.size()) throw ex;
		Iterator<String> i = this.row.keySet().iterator();
		Iterator<String> j = row.keySet().iterator();
		while (i.hasNext()) {
			j.hasNext();
			if (!i.next().equals(j.next())) throw ex;
		}
		i = this.row.keySet().iterator();
		j = row.keySet().iterator();
		while (i.hasNext()) {
			j.hasNext();
			System.out.println(this.row.get(i.next()).getClass());
			System.out.println(row.get(j.next()).getClass());
			if (this.row.get(i.next()).getClass() !=
					row.get(j.next()).getClass()) throw ex;
		}
		/**
		 * Compose an prepared INSERT statement.
		 */
		StringBuilder sql = new StringBuilder("INSERT INTO ");
		sql.append(tableName);
		// Append the list of column names.
		String delimiter = "(";
		Set<String> keys = row.keySet();
		i = keys.iterator();
		while (i.hasNext()) {
			sql.append(delimiter);
			sql.append(i.next());
			delimiter = ",";
		}
		// Append the place holders for the field values.
		sql.append(") VALUES ");
		delimiter = "(";
		keys = row.keySet();
		i = keys.iterator();
		while (i.hasNext()) {
			sql.append(delimiter);
			/**
			 * Here is where enum values belonging to this
			 * package are converted to database enum values.
			 */
			String key = i.next();
			Object o = row.get(key);
			Class c = o.getClass();
			String s = c.getName();
			if (c.isEnum() && s.startsWith(packageName)) {
				sql.append("CAST(? AS ");
				sql.append(s.substring(packageName.length()+1).toLowerCase());	// +1 for the dot
				sql.append(")");
				row.put(key,o.toString());
			} else {
				sql.append("?");
			}
			delimiter = ",";
		}
		// Avoid exceptions, and ask for the primary key of the new row.
		sql.append(") ON CONFLICT DO NOTHING RETURNING ");
		sql.append(primaryKey);
		sql.append(";");
		// Compile the prepared statement with the field values.
		PreparedStatement ps = getConnection().prepareStatement(sql.toString());
		int k = 0;
		i = keys.iterator();
		while (i.hasNext()) ps.setObject(k+=1, row.get(i.next()));
		// Execute the query and retrieve the primary key of the new row.
		ResultSet rs = ps.executeQuery();
		return rs.next() ? rs.getInt(primaryKey) : -1;
	}

	public Row select (int user_id) throws Exception {
		String sql = "SELECT * FROM users WHERE user_id = ?;";
		PreparedStatement ps = getConnection().prepareStatement(sql);
		ps.setInt(1, user_id);
		ResultSet rs = ps.executeQuery();
		if (!rs.next()) return null;
		Row row = getRow();
		Set<String> keys = row.keySet();
		Iterator<String> i = keys.iterator();
		while(i.hasNext()) {
			String key = i.next();
			row.put(key, rs.getString(key));
		};
		return row;
	}

}
