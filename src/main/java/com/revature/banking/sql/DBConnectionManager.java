package com.revature.banking.sql;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// In lieu of an actual user interface
class LocalUserInfo implements UserInfo {
	String passwd;
	public String getPassword() { return passwd; }
	public boolean promptYesNo(String str) { return true;}
	public String getPassphrase() { return null; }
	public boolean promptPassphrase(String message) { return true; }
	public boolean promptPassword(String message) { return true; }
	public void showMessage(String message) {}
}

public class DBConnectionManager implements AutoCloseable {
	private static final String dbURLRegex =
			"^jdbc:([a-z]+):([a-z]+:)*//([a-z]+(\\.[a-z]+)*)(:([1-9][0-9]*))?/(.*)$";
	private static final Pattern dbURLPattern = Pattern.compile(dbURLRegex);
	private static final String publicKeyType = "ssh-rsa";
	private static final int mysqlDefaultPort = 3306;
	private static final int postgresDefaultPort = 5432;

	private static String jdbcDriver, dbURL, dbUsername, dbPassword;
	private static String dbDialect, dbHost, dbSchema;
	private static int dbPort=0;
	private static String sshUsername, sshPassword, sshHomeFolder;
	private static Session session;
	private static int forwardedPort;
	private static Connection conn = null;
	
    private static void parseURL (String url) throws Exception {
    	Matcher m = dbURLPattern.matcher(url);
    	if (!m.matches()){
			throw new Exception("Property 'dbURL' is an invalid URI.");
		}
		dbDialect = m.group(1);
		if (dbDialect.equals("mysql")) dbPort = mysqlDefaultPort;
		if (dbDialect.equals("postgres")) dbPort = postgresDefaultPort;
		dbHost = m.group(3);
		String s = m.group(6);
		if (s != null && !s.isEmpty()) dbPort = Integer.parseInt(s);
		if (dbPort == 0) {
			throw new Exception("Property 'dbURL' has an invalid port number.");
		}
		dbSchema = m.group(7);
    }
    
	public static final void reportException (Exception ex) {
		System.err.println(ex.getClass().getName()+":"+ex.getMessage());
		ex.printStackTrace();
	}
	public static final void bolt (Exception exception) {
		reportException(exception);
		try {
			instance.close();
		} catch (Exception ex) {	// resource cannot be closed
			reportException(ex);
		}
		System.exit(1);
	}

	 // For now, this class only supports one connection manager at a time.
	private DBConnectionManager () {}
    private static DBConnectionManager instance = new DBConnectionManager();
	public static DBConnectionManager getInstance() { return instance; }
	
	public static void initialize (String propertiesFile) throws Exception {
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(propertiesFile));
		} catch (Exception ex) {
			System.err.println("Cannot read file "+propertiesFile+".");
			bolt(ex);
		}
		jdbcDriver = properties.getProperty("jdbcDriver");
		if (jdbcDriver == null) {
			bolt(new Exception("Missing required property '"+
					"jdbcDriver' in file "+propertiesFile+"."));
		}
		// Load driver.
	    try {
	        Class.forName(jdbcDriver);
	    } catch (ClassNotFoundException ex) {
	    	System.err.println("Database driver "+jdbcDriver+" is not available.");
	    	bolt(ex);
	    	throw ex;
	    }
	    // Get the arguments needed for DriverManager.getConnection
		dbURL = properties.getProperty("dbURL");
		if (dbURL == null) {
			bolt(new Exception("Missing required property '"+
					"dbURL' in file "+propertiesFile+"."));
		}
		parseURL(dbURL); 
		dbUsername = properties.getProperty("dbUsername");
		if (dbUsername == null) {
			bolt(new Exception("Missing required property '"+
					"dbUsername' in file "+propertiesFile+"."));
		}
		dbPassword = properties.getProperty("dbPassword");
		if (dbPassword == null) {
			bolt(new Exception("Missing required property '"+
					"dbPassword' in file "+propertiesFile+"."));
		}
		// Properties for a SSH tunnel which will become standard when it works.
		sshUsername = properties.getProperty("sshUsername");
		sshPassword = properties.getProperty("sshPassword");
		sshHomeFolder = properties.getProperty("sshHomeFolder");
		if (sshHomeFolder != null && sshUsername != null && sshPassword != null) {
    		JSch jsch=new JSch();
    		Session session = null;
    		try {
    			session = jsch.getSession(sshUsername, dbHost);	// port defaults to 22
	    		session.setPassword(sshPassword);
	    		LocalUserInfo lui = new LocalUserInfo();
	    		session.setUserInfo(lui);
    			session.connect();
    			forwardedPort = session.setPortForwardingL(0, "localhost", dbPort);
        		System.out.println(forwardedPort);
    		} catch (JSchException ex) {
    			bolt(ex);
    		}
    		System.out.println(session);
    		String[] pfL = session.getPortForwardingL();
    		for (int i=0; i<pfL.length; i+=1) System.out.println(pfL[i]);
		}
	    // Get the first connection.
	    DBConnectionManager.conn = getConnection();
    }
	

    public static Connection getConnection() {
    	try {
	    	if (conn == null || conn.isClosed())
	    		conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
	    } catch (SQLException ex) {
	    	// handle any exception
	    	System.err.println("SQLException: " + ex.getMessage());
	    	System.err.println("SQLState: " + ex.getSQLState());
	    	System.err.println("VendorError: " + ex.getErrorCode());
	    	bolt(ex);
	    } catch (Exception ex) {
    		bolt(ex);
    	}
    	return conn;
    }
    
    public void close () throws SQLException, JSchException {
    	if (conn != null && !conn.isClosed()) conn.close();
    	if (session != null) {
        	System.err.println(session);
        	System.err.println(forwardedPort);
	    	session.delPortForwardingL(forwardedPort);
	    	session.disconnect();
    	}
    }
    
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Exactly one argument is required.");
			System.exit(1);
		}
		try {
	    	DBConnectionManager.initialize(args[0]);
	    	Connection conn = DBConnectionManager.getConnection();
			System.out.println(conn.getSchema());
	    } catch (SQLException ex) {
	    	// handle any errors
	    	System.err.println("SQLException: " + ex.getMessage());
	    	System.err.println("SQLState: " + ex.getSQLState());
	    	System.err.println("VendorError: " + ex.getErrorCode());
    		bolt(ex);
	    } catch (Exception ex) {
    		bolt(ex);
    	} finally {
    		//DBConnectionManager.getInstance().close();
    	}
    }
}
