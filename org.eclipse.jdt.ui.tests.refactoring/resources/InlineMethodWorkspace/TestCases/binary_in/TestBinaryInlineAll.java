package binary_in;

import classes.Target;
import classes.Target2;

public class TestBinaryInlineAll {
	void use() {
		int len = Target2.logMessage("Hello");
		System.err.println("logged " + len + " chars");
	}
	int use2() {
		return Target2.logMessage("Hi");
	}
	int doesNotUse() {
		return new Target().logMessage("Go");
	}
}
