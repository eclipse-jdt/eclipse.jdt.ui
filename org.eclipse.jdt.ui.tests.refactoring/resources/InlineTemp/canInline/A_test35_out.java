package p;

import java.util.List;

class A {
	void x() {
		for (Number n : (List<? extends Number>) getNums()) {
			System.out.println(n);
		}
	}

	private List getNums() {
		return null;
	}
}
