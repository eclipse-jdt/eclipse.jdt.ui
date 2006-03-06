package binary_out;

import classes.Target;
import classes.Target2;

public class TestBinaryInlineAll {
	void use() {
		int len = new Target().logMessage("Hello");
		System.err.println("logged " + len + " chars");
	}
	int use2() {
		return new Target().logMessage("Hi");
	}
	int doesNotUse() {
		return new Target().logMessage("Go");
	}
}
