package bugs_out;

import java.util.HashMap;

public class Test_95128 extends HashMap<String, String> {
	private static final long serialVersionUID = 1L;

	private String useOne(String s) {
		return this.get(s);
	}
	
	private static void useTwo() {
		Test_95128 test= null;
		String s= test.get(null);
	}
}
