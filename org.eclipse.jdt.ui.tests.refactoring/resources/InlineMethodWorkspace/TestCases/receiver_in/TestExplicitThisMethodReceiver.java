package receiver_in;

public class TestExplicitThisMethodReceiver {
	protected Logger4 getLogger() {
		return Logger.getLogger("");
	}
    protected Logger4 getLOG() {
        return getLogger();
    }
    public void ref() {
    	this./*]*/getLOG()/*[*/.info("message");
    }
}

class Logger4 {
	public static Logger4 getLogger(String string) {
		return null;
	}
	public void info(String string) {
	}
}