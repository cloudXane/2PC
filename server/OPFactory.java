package server;

import util.Setting;

public class OPFactory {

    public static Operation create(String opName) {

	if (opName.equals(Setting.TRANSFER)) {
	    return new OPTransfer();

	} else
	    return null;
    }

}
