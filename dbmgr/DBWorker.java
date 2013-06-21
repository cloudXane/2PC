package dbmgr;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.*;

import util.Setting;

/**
 * connect with MySQL,
 * in current version, DBWorker does not support overlapped 
 * submitting. Because DBWorker just servers the TManager, 
 * which issues transaction to each AppServer ONE BY ONE 
 * */

public abstract class DBWorker {
    Connection connection = null;

    ResultSet  resultSet  = null;
    PreparedStatement prepstat = null;

    public DBWorker () {
	connect();
    }

    void connect() {
	try {
	    Class.forName("com.mysql.jdbc.Driver");
	    connection = DriverManager.getConnection("jdbc:" 
		    + "mysql://localhost/" + Setting.DB_NAME + "?"
		    + "user=" + Setting.DB_USER + "&" 
		    + "password=" + Setting.DB_PASS);

	    connection.setAutoCommit(false);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /* template function */
    abstract int __prep(String params[]);

    int prep(String params[]) {
	System.out.println("tentative exec tran");

	if (params.length < 2) 
	    return Setting.NO;

	return __prep(params);
    }

    int abort() {
	System.out.println("abort tran");

	int ret = Setting.YES;
	try {
	    connection.rollback();

	    if (prepstat != null)
		prepstat.close();

	} catch (SQLException e) {
	    e.printStackTrace();
	    ret = Setting.NO;
	}

	return ret;
    }

    int commit() {
	System.out.println("commit tran");

	int ret = Setting.YES;
	try {
	    connection.commit();

	    if (prepstat != null)
		prepstat.close();

	} catch (SQLException e) {
	    e.printStackTrace();
	    ret = Setting.NO;
	}

	return ret;
    }

    void close() {
	try {
	    if (resultSet != null) {
		resultSet.close();
	    }

	    if (prepstat != null) {
		prepstat.close();
	    }

	    if (connection != null) {
		connection.close();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
