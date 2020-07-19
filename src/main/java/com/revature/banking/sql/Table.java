package com.revature.banking.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * For simplicity and elegance, this Table class is designed to operate
 * on each table only one row at a time.  It requires that the table have
 * a non-composite integer primary key.  Assigning a value to a primary
 * key is not the task of this class.  It is up to the DBMS to provide
 * that service.
 * 
 * @author David N. Jafferian
 *
 */
public abstract class Table implements DBContext {
	public Connection getConnection() {
		return DBConnectionManager.getConnection();
	}
	
	private static final String sqlSelectColumnCount =
			"SELECT COUNT(*) "+
			"FROM information_schema.columns "+
			"WHERE table_name = ?";
	private static final String sqlSelectColumnNames =
			"SELECT column_name, data_type, udt_name "+
			"FROM information_schema.columns "+
			"WHERE table_name = ?";
	private static String packageName = Table.class.getPackageName();
	
	private String tableName;
	private String primaryKey;
	public String getTableName () { return tableName; }
	public String getPrimaryKey () { return primaryKey; }
	
	/**
	 * During each instantiation of a Table object a sample row is created
	 * to provide a template for inserts and updates and to deliver results
	 * from selects.  Although it is exposed, only the values are mutable,
	 * so the Table class can verify whether a row submitted as input is
	 * the same supplied by getRow, or at least a faithful copy.
	 */
	private Row row;
	public Row getRow() { return row; }
	private String strColumnNameList;
	private String strPlaceHolders;
	private String whereClause;
	private String deleteStatement;

	public Table(String tn, String pk) throws Exception {
		tableName = tn;
		primaryKey = pk;
		whereClause = " WHERE "+primaryKey+" = ?;";
		deleteStatement = "DELETE FROM "+tableName+whereClause;
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
		StringBuilder sbCols = new StringBuilder(n*20);
		StringBuilder sbVals = new StringBuilder(n*3);
		String delimiter = "(";
		ps = getConnection().prepareStatement(sqlSelectColumnNames);
		ps.setString(1, tableName);
		rs = ps.executeQuery();
		while (rs.next()) {
			String columnName = rs.getString(1);
			if (columnName.equals(primaryKey)) continue;
			sbCols.append(delimiter);
			sbCols.append(columnName);
			sbVals.append(delimiter);
			sbVals.append("?");	// placeholder
			delimiter = ",";
			// Convert certain database types into Java object types.
			String dataType = rs.getString(2);
			Object o = null;
			switch (dataType) {
			case "integer" :
				o = Integer.valueOf(0);
				break;
			case "character varying" :
				o = new String();
				break;
			// ENUM
			case "USER-DEFINED" :
				String udt_name = rs.getString(3);
				String className = "com.revature.banking.sql." +
						udt_name.substring(0,1).toUpperCase() +
						udt_name.substring(1);
				Class<?> c = Class.forName(className);
				if (!c.isEnum()) throw new Exception("Bizzare stuff going on in here.");
				/**
				 * If the column data type is an ENUM then this must be
				 * represented as a string type cast to the ENUM value,
				 * i.e. CAST("value" AS enum_name), in standard SQL or
				 * this double colon syntax in Postgres :
				 */
				o = c.getEnumConstants()[0];
				sbVals.append("::");
				sbVals.append(udt_name);
				break;
			}
			row.put(columnName, o);
		}
		sbCols.append(")");
		sbVals.append(")");
		strColumnNameList = sbCols.toString();
		strPlaceHolders = sbVals.toString();
		this.row = new Row(row);
	}
	
	/**
	 * A few helper methods.
	 */
	private void validateRowAsTemplate (Row row) throws SQLException {
		SQLException ex = new SQLException(
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
			if (this.row.get(i.next()).getClass() !=
					row.get(j.next()).getClass()) throw ex;
		}
	}
	
	private int fillInPlaceHolders (
			PreparedStatement ps, Row row) throws SQLException {
		Set<String> keys = row.keySet();
		Iterator<String> i = keys.iterator();
		int j = 1;
		while (i.hasNext()) {
			Object o = row.get(i.next());
			if (o.getClass().isEnum()) {
				ps.setString(j, o.toString());
			} else {
				ps.setObject(j, o);
			}
			j+=1;
		}
		return j;
	}

	private void fillInPlaceHolders (
			PreparedStatement ps, Row row, int pk_id) throws SQLException {
		int j = fillInPlaceHolders(ps, row);
		ps.setInt(j, pk_id);
	}

	public int create (Row row) throws SQLException {
		/**
		 * First, make sure that the row passed here, came from here.
		 */
		validateRowAsTemplate(row);
		/**
		 * Compose a prepared INSERT statement.
		 */
		StringBuilder sql = new StringBuilder("INSERT INTO ");
		sql.append(tableName);
		sql.append(strColumnNameList);
		sql.append(" VALUES ");
		sql.append(strPlaceHolders);
		// Avoid exceptions, and ask for the primary key of the new row.
		sql.append(" ON CONFLICT DO NOTHING RETURNING ");
		sql.append(primaryKey);
		sql.append(";");
		// Compile the prepared statement with the field values.
		PreparedStatement ps = getConnection().prepareStatement(sql.toString());
		fillInPlaceHolders(ps, row);
		// Execute the query and retrieve the primary key of the new row.
		ResultSet rs = ps.executeQuery();
		return rs.next() ? rs.getInt(primaryKey) : 0;
	}

	public Row read (int pk_id) throws SQLException {
		String sql = "SELECT * FROM "+tableName+whereClause;
		PreparedStatement ps = getConnection().prepareStatement(sql);
		ps.setInt(1, pk_id);
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
	
	public void update (int pk_id, Row row) throws SQLException {
		validateRowAsTemplate(row);
		/**
		 * Compose a prepared UPDATE statement.
		 */
		StringBuilder sql = new StringBuilder("UPDATE ");
		sql.append(tableName);
		sql.append(" SET ");
		// Append the list of column names.
		sql.append(strColumnNameList);
		sql.append(" = ");
		sql.append(strPlaceHolders);
		// Specify which row is to be updated.
		sql.append(whereClause);
		// Compile the prepared statement with the field values.
		PreparedStatement ps = getConnection().prepareStatement(sql.toString());
		fillInPlaceHolders(ps, row, pk_id);
		// Execute the query.
		ps.executeUpdate();				
	}
	
	public void delete (int pk_id) throws Exception {
		PreparedStatement ps = getConnection().prepareStatement(deleteStatement);
		ps.setInt(1, pk_id);
		ps.executeUpdate();				
	}
}
