package bugs_in;

import java.util.HashMap;

public class Test_95128 extends HashMap<String, String> {
	private static final long serialVersionUID = 1L;

	public HashMap<String, String> toInline() {
		return this;
	}
	
	private String useOne(String s) {
		return toInline().get(s);
	}
	
	private static void useTwo() {
		Test_95128 test= null;
		String s= test.toInline().get(null);
	}
}
