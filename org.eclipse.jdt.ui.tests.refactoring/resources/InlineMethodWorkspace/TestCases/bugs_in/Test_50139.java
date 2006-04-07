package bugs_in;

public class Test_50139 {
	static class Provider {
	    public static final String ID= "id";

	    public static String getId() {
	        return ID;
	    }
	}
	
    public void foo() {
        String s= Provider./*]*/getId()/*[*/;
    }
}
