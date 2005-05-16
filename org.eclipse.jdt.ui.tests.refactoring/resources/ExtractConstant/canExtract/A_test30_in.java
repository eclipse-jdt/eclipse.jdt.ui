//12, 19, 12, 28
package p;

import java.util.ArrayList;

class A_30_Super {
	static final ArrayList<? super Integer> NL= new ArrayList<Integer>();
}

class A extends A_30_Super {
	void foo() {
		Object o= NL.get(0);
	}
}
