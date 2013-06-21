package dbmgr;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.*;

import util.Setting;
import util.MyLogger;

public class CBRCAppSvr extends AppServer {
    public static final String NAME = Setting.CBRC_NAME;
    public static final int    PORT = Setting.CBRC_PORT;

    /* Main thread do the listening stuff */
    public static void main(String[] args) { 
	new CBRCAppSvr().start();
    }

    public CBRCAppSvr() {
	super(NAME, true);
	this.svrPort = PORT;
    }

    public void start() {
	LOG2PC = MyLogger.getInstance(CBRCAppSvr.class.getName());
	dbwk = new CBRC_DBWorker();
	new Listener().start();
    }

    class CBRC_DBWorker extends DBWorker {

	int __prep(String params[]) {

	    int ret = Setting.NO;

	    switch (params[1]) {

		case Setting.MOVE:
		    ret = audit_move(params[2], 
			    Integer.parseInt(params[3]),
			    params[4], params[5]);
		    break;

		default:
		    System.err.println("Unknown operation " 
			    + params[1]);
		    break;
	    }

	    return ret;
	}

	int audit_move(String uname, int amount, String fromBK, 
		String toBK) {

	    int ret;

	    String sql = "INSERT INTO DEAL VALUES (default, ?, ?, ?, ?, default);";

	    System.out.println(sql + " [" + uname + ", " + amount 
		    + ", " + fromBK + ", " + toBK + "]");

	    try {
		prepstat = connection.prepareStatement(sql);
		prepstat.setString(1, uname);
		prepstat.setInt(2, amount);
		prepstat.setString(3, fromBK);
		prepstat.setString(4, toBK);

		ret = prepstat.executeUpdate();

		if (ret <= 0)
		    ret = Setting.NO;
		else
		    ret = Setting.YES;

	    } catch (SQLException e) {
		e.printStackTrace();

		ret = Setting.NO;
	    }

	    return ret;
	}
    }
}
