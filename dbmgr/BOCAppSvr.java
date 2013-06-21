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

public class BOCAppSvr extends AppServer {

    public static final String NAME = Setting.BOC_NAME;
    public static final int    PORT = Setting.BOC_PORT;

    /* Main thread do the listening stuff */
    public static void main(String[] args) { 
	new BOCAppSvr().start();
    }

    public BOCAppSvr() {
	super(NAME);
	this.svrPort = PORT;
    }

    public void start() {
	LOG2PC = MyLogger.getInstance(BOCAppSvr.class.getName());

	dbwk = new BOC_DBWorker();
	new Listener().start();
    }

    class BOC_DBWorker extends DBWorker {

	int __prep(String params[]) {

	    int ret = Setting.NO;

	    switch (params[1]) {

		case Setting.MOVE_OUT:
		    ret = move(-1, params[2], 
			    Integer.parseInt(params[3]));
		    break;

		case Setting.MOVE_IN:
		    ret = move(1, params[2], 
			    Integer.parseInt(params[3]));
		    break;

		default:
		    System.err.println("Unknown operation " 
			    + params[1]);
		    break;
	    }

	    return ret;
	}

	int move(int inout, String uname, int amount) {
	    int ret;

	    String sql = "";

	    if (inout < 0) /* money move out */
		sql="UPDATE BOCACCOUNT SET BALANCE=BALANCE-? WHERE NAME=?";
	    else if (inout > 0) /* money move in */
		sql="UPDATE BOCACCOUNT SET BALANCE=BALANCE+? WHERE NAME=?";

	    System.out.println(sql + " [" + uname + ", " + amount + "]");

	    try {

		prepstat = connection.prepareStatement(sql);
		prepstat.setInt(1, amount);
		prepstat.setString(2, uname);

		/* not commit yet, return the number of 
		 * rows affected. At least one is affected */
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
