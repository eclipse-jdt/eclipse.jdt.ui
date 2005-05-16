//12, 19, 12, 28
package p;

import java.util.ArrayList;

class A_29_Super {
	static final ArrayList<? extends Number> NL= new ArrayList<Integer>();
}

class A extends A_29_Super {
	private static final Number NUMBER= NL.get(0);

	void foo() {
		Number n= NUMBER;
	}
}
