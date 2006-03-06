package binary_in;

import classes.Target;

public class TestBinaryInlineSingle {
	void use() {
		int len = new Target()./*]*/logMessage/*[*/("Hello");
		System.err.println("logged " + len + " chars");
	}
}
