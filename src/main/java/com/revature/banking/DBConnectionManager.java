package com.revature.banking;

import com.jcraft.jsch.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;

public class DBConnectionManager {
	/**
	 *	For testing; these parameters should not have defaults.
	 *	The postgresql default server host and port is localhost:5432.
	 *	See https://jdbc.postgresql.org/documentation/head/connect.html
	 **/
	// Default config is a Postgres database on the local host.
	private static final String defaultDriver = "org.postgresql.Driver";
	private static final String defaultDatabase = "jdbc:postgresql:postgres";
	private static final String defaultUsername = "postgres";
	private static final String defaultPassword = "Postgres5099";
	// Remote option is a MySQL database bound to a remote host.
	private static final String sshHomeFolder = "C:\\Users\\Owner\\.ssh\\";
	private static final String identityFile = sshHomeFolder+"id_rsa";
	private static final String knownHostsFile = sshHomeFolder+"known_hosts";
	private static final String publicKeyType = "ssh-rsa";
	private static final String remoteHost = "debian";
	private static final String remoteUser = "david";
	private static final String remotePassword = "dna5099";
	private static final int mysqlPort = 3306;
	private static final String mysqlDriver = "com.mysql.cj.jdbc.Driver";
	private static final String mysqlUsername = "root";
	private static final String mysqlPassword = "Mysql5099!";
	
	// For now, this class only supports one connection at a time.
	private static String driver, database, username, password;
	private static Connection conn;
	
	public static void main(String[] args) throws Exception {
    	// For testing
    	try {
	    	if (args.length < 1) {
	    		new DBConnectionManager(defaultDriver,
	    				defaultDatabase, defaultUsername, defaultPassword);    		
	    	} else if (args[0].equals("remote")) {
	    		JSch jsch=new JSch();
	    		jsch.addIdentity(identityFile);
	    	    jsch.setKnownHosts(knownHostsFile);
	    		HostKeyRepository hkr=jsch.getHostKeyRepository();
	    	    HostKey[] hks=hkr.getHostKey(remoteHost, publicKeyType);
	    	    if (hks==null)
	    	    	throw new Exception("The file '"+knownHostsFile+
	    	    			"' does not contain a record for the host '"+remoteHost+
	    	    			"' using a public key of the type '"+
	    	    		"No 'ssh-rsa' key type found for host "+remoteHost+".");
	    		Session session=jsch.getSession(remoteUser, remoteHost);	// port defaults to 22
	    		session.setPassword(remotePassword);
	    		session.setDaemonThread(true);
	    		session.connect();
	    		int forwardedPort = session.setPortForwardingL(0, "localhost", 3306);
	    		System.out.println(forwardedPort);
	    		String[] pfL = session.getPortForwardingL();
	    		for (int i=0; i<pfL.length; i+=1) System.out.println(pfL[i]);
	    		String[] pfR = session.getPortForwardingR();
	    		for (int i=0; i<pfR.length; i+=1) System.out.println(pfR[i]);
	    		new DBConnectionManager(mysqlDriver,
	    				"jdbc:mysql://"+remoteHost+":"+forwardedPort+"/test",
	    				mysqlUsername, mysqlPassword);    		
	    		// clean up
	    		session.delPortForwardingL(1111);
	    		session.disconnect();
	    	} else {
	    		throw new IllegalArgumentException("Illegal argument passed to main.");
	    	}
	    } catch (SQLException ex) {
	    	// handle any errors
	    	System.err.println("SQLException: " + ex.getMessage());
	    	System.err.println("SQLState: " + ex.getSQLState());
	    	System.err.println("VendorError: " + ex.getErrorCode());
	    } catch (Exception ex) {
    		System.err.println(ex.getClass().getName()+": " + ex.getMessage());
    	}
    }

    @SuppressWarnings("deprecation")
	private DBConnectionManager (String dr, String db, String un, String pw) throws Exception {
		driver = dr;
		database = db;  System.err.println(db);
		username = un;
		password = pw;
	    // Load the driver.
	    try {
	        // The newInstance() call is a work around for some
	        // broken Java implementations
	        Class.forName(driver).newInstance();
	    } catch (ClassNotFoundException ex) {
	    	System.err.println("ClassNotFoundException: " + ex.getMessage());
	    	System.err.println("Database driver "+driver+" is not available.");
	    }
	    // Get the first connection.
	    try (Connection conn = DriverManager.getConnection(database, username, password)) {
	    	DBConnectionManager.conn = conn;
	    }
    }
    
    public static Connection getConnection() throws SQLException {
    	if (DBConnectionManager.conn.isClosed()) {
		    try (Connection conn = DriverManager.getConnection(database, username, password)) {
		    	DBConnectionManager.conn = conn;
		    }
    	}
	    return DBConnectionManager.conn;
    }
}
