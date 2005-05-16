//12, 19, 12, 28
package p;

import java.util.ArrayList;

class A_30_Super {
	static final ArrayList<? super Integer> NL= new ArrayList<Integer>();
}

class A extends A_30_Super {
	private static final Object INTEGER= NL.get(0);

	void foo() {
		Object o= INTEGER;
	}
}
