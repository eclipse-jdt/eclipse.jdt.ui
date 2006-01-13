package p0;

import p1.Other;

public class Test {
	void m() {
		Other.bar(new StringBuffer("hello"), 1, 3);
		Other.bar(new StringBuffer("hello"), 1, 4);
	}
}
