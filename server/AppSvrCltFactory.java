package server;

import util.Setting;

public class AppSvrCltFactory {

    public static AppSvrClt create(String asName) {
	if (asName.equals(Setting.BOC_NAME)) {
	    return new BOCAppSvrClt();

	} else if (asName.equals(Setting.CCB_NAME)) {
	    return new CCBAppSvrClt();

	} else if (asName.equals(Setting.CBRC_NAME)) {
	    return new CBRCAppSvrClt();

	} else
	    return null;
    }
}
