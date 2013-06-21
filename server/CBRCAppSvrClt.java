package server;

import util.Setting;

public class CBRCAppSvrClt extends AppSvrClt {
    public static final String NAME = Setting.CBRC_NAME;

    public CBRCAppSvrClt() {
	super(NAME, true);

	this.svrHost = Setting.CBRC_HOST;
	this.svrPort = Setting.CBRC_PORT;

	start();
    }
}
