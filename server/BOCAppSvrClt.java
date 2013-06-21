package server;

import util.Setting;

public class BOCAppSvrClt extends AppSvrClt {
    public static final String NAME = Setting.BOC_NAME;

    public BOCAppSvrClt() {
	super(NAME);

	this.svrHost = Setting.BOC_HOST; 
	this.svrPort = Setting.BOC_PORT;

	start();
    }
}
