package binary_out;

import classes.Target;

public class TestBinaryInlineSingle {
	void use() {
		Target r = new Target();
		System.out.println("Hello");
		int len = "Hello".length();
		System.err.println("logged " + len + " chars");
	}
}
