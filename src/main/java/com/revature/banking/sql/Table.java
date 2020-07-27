package com.revature.banking.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletException;

/**
 * For simplicity and elegance, this Table class is designed to operate
 * on each table only one row at a time.  It requires that the table have
 * a non-composite integer primary key.  Assigning a value to a primary
 * key is not part of the task of this class.  It is up to the DBMS to
 * provide that service.
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
			"WHERE table_name = ? "+
			"ORDER BY ordinal_position";
	private static final String packageName = Table.class.getPackageName();
	
	private final String tableName;
	private final String primaryKey;
	private final String whereClause;
	private final String countStatement;
	private final String deleteStatement;
	protected final String readAllStatement;
	private final String readOneStatement;
	private final String strColumnNameList;
	private final String strPlaceHolders;
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
	public Row getRow() { return new Row(row); }
	
	/** 
	 * In the String argument to the Connection.prepareStatement method, the
	 * question mark, '?', is used as a placeholder for most values which are
	 * inserted by means of a PreparedStatement setter method particular to
	 * the type of the value.  But there is no setter method for enum types.
	 * To work around this, the enum value is converted to a string which is
	 * later type cast back to the enum type it was.  This is done by wrapping
	 * the question mark in the syntax for a type cast, "CAST(? AS enum_name)".
	 * (Postgres has an alternate syntax, "?::enum_name", which is arguably
	 * superior yet not portable.)  The placeholder replacements are stored
	 * for reuse in placeHolders. 
	 */
	private Map<String,String> placeHolders = new HashMap<String,String>();
	
	public Table(String tn, String pk) throws SQLException {
		tableName = tn;
		primaryKey = pk;
		whereClause = " WHERE "+pk+" = ?";
		countStatement = "SELECT COUNT(*) c FROM "+tn;
		deleteStatement = "DELETE FROM "+tn+whereClause;
		readAllStatement = "SELECT * FROM "+tn;
		readOneStatement = readAllStatement+whereClause;
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
		LinkedHashMap<String,Object> row = new LinkedHashMap<String,Object>(n);
		StringBuilder sbCols = new StringBuilder(n*20);
		StringBuilder sbVals = new StringBuilder(n*3);
		String delimiter = "(";
		ps = getConnection().prepareStatement(sqlSelectColumnNames);
		ps.setString(1, tableName);
		rs = ps.executeQuery();
		while (rs.next()) {
			String columnName = rs.getString(1);
			if (columnName.equals(primaryKey)) {
				row.put(columnName, Integer.valueOf(0));
				continue;
			}
			sbCols.append(delimiter);
			sbCols.append(columnName);
			sbVals.append(delimiter);
			String placeholder = "?";
			// Convert certain database types into Java object types.
			String dataType = rs.getString(2);
			Object o = null;
			switch (dataType) {
			case "character varying" :
				o = new String();
				break;
			case "integer" :
				o = Integer.valueOf(0);
				break;
			case "numeric" :
				o = Double.valueOf(0);
				break;
			// ENUM
			case "USER-DEFINED" :
				String udt_name = rs.getString(3);
				String className = packageName+"."+
						udt_name.substring(0,1).toUpperCase() +
						udt_name.substring(1);
				Class<?> c = null;
				try {
					c = Class.forName(className);
				} catch (ClassNotFoundException cnfe) {
					System.err.println("Does class "+className+" exist ?");
					System.exit(1);
				}
				assert c.isEnum();
				o = c.getEnumConstants()[0];
				placeholder = "CAST(? AS "+udt_name+")";
				//placeholder = "?::+udt_name;
				placeHolders.put(columnName, placeholder);
				break;
			}
			sbVals.append(placeholder);
			row.put(columnName, o);
			delimiter = ",";
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
			String thiskey = i.next(), thatkey = j.next();
			System.out.println(this.row.get(thiskey).getClass());
			System.out.println(row.get(thatkey).getClass());
			if (this.row.get(thiskey).getClass() !=
					row.get(thatkey).getClass()) throw ex;
		}
	}
	
	private int fillInPlaceHolders (
			PreparedStatement ps, Row row) throws SQLException {
		Set<String> keys = row.keySet();
		Iterator<String> i = keys.iterator();
		int j = 1;
		while (i.hasNext()) {
			String key = i.next();
			if (key.equals(primaryKey)) continue;
			Object o = row.get(key);
			if (o.getClass().isEnum()) {
				ps.setString(j, o.toString());
			} else {
				ps.setObject(j, o);
			}
			j+=1;
		}
		return j;
	}

	protected final Row resultSetRowToRow (ResultSet rs) throws SQLException {
		Row row = getRow();
		Iterator<String> i = row.keySet().iterator();
		for (int j=1; i.hasNext(); j+=1) {
			String key = i.next();
			Object o = row.get(key);
			Class<?> c = o.getClass();
			if (c.isEnum()) {
				String s = rs.getString(j);
				for (Object ec : c.getEnumConstants())
					if (s.equals(ec.toString())) { o = ec; break; }
			} else {
				o = rs.getObject(j);
			}
			System.out.println(key+":"+o);
			row.put(key, o);
		}
		return row;
	}

	/***
	 * There are two ways to specify an INSERT statement to ensure that each
	 * primary key receives its default, auto-generated value, by explicitly
	 * using the keyword DEFAULT as the "value" of the primary key, or leaving
	 * out the primary key altogether.  The latter is more portable, so that
	 * is what is done here.  The consequence is that the implicit order of
	 * the columns is broken, and the list of columns cannot be excluded from
	 * the syntax of the INSERT statement.
	 * 
	 * @param row
	 * @return
	 * @throws SQLException
	 */
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
		sql.append(strColumnNameList);	// Optional
		sql.append(" VALUES ");
		sql.append(strPlaceHolders);
		/**
		 * Avoid exceptions, and ask for the primary key of the new row.
		 * Postgres has an option "RETURNING *" which returns the entire row,
		 * which would be preferred for our purposes, but MySQL has nothing
		 * as simple.  So we will be content to execute a second statement
		 * to get what we prefer.
		 */
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
	
	public Row readOne (int pk_id) throws SQLException {
		PreparedStatement ps = getConnection().prepareStatement(readOneStatement);
		ps.setInt(1, pk_id);
		ResultSet rs = ps.executeQuery();
		if (!rs.next()) return null;
		return resultSetRowToRow(rs);
	}
	
	/**
	 * The 'readAll' method retrieves a list of all of the rows in this table.
	 * The DBMS ensures that the primary keys are a set, so storing the elements
	 * in a ArrayList is faster than storing them into a Set.
	 * 
	 * Counting the number of rows before retrieving the keys is not reliable,
	 * and useful only to estimate the needed capacity of the array.
	 * 
	 * @return ArrayList<Row>
	 * @throws SQLException
	 */
	private int count () throws SQLException  {
		PreparedStatement ps = getConnection().prepareStatement(countStatement);
		ResultSet rs = ps.executeQuery();
		return rs.next() ? rs.getInt("c") : 0;
	}
	public ArrayList<Row> readAll () throws SQLException {
		PreparedStatement ps = getConnection().prepareStatement(readAllStatement);
		ArrayList<Row> set = new ArrayList<Row>(count()+100);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) set.add(resultSetRowToRow(rs));
		return set;
	}
	
	public ArrayList<Row> readSome (String colName, Object value) throws SQLException {
		String placeholder = placeHolders.get(colName);
		if (placeholder == null) placeholder = "?";
		else value = value.toString();
		String sql = readAllStatement+" WHERE "+colName+" = "+placeholder;
		PreparedStatement ps = getConnection().prepareStatement(sql);
		ps.setObject(1, value);
		ArrayList<Row> set = new ArrayList<Row>();
		ResultSet rs = ps.executeQuery();
		while (rs.next()) set.add(resultSetRowToRow(rs));
		return set;
	}

	public void update (Row row) throws SQLException {
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
		// Replace the last place holder with the primary key, to complete the WHERE clause.
		ps.setInt(fillInPlaceHolders(ps, row), ((Integer)(row.get(primaryKey))).intValue());
		// Execute the query.
		ps.executeUpdate();				
	}
	
	public void delete (int pk_id) throws SQLException {
		PreparedStatement ps = getConnection().prepareStatement(deleteStatement);
		ps.setInt(1, pk_id);
		ps.executeUpdate();				
	}
}
