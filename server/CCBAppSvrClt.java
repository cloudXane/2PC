package server;

import util.Setting;

public class CCBAppSvrClt extends AppSvrClt {
    public static final String NAME = Setting.CCB_NAME;

    public CCBAppSvrClt() {
	super(NAME);

	this.svrHost = Setting.CCB_HOST;
	this.svrPort = Setting.CCB_PORT;

	start();
    }
}
