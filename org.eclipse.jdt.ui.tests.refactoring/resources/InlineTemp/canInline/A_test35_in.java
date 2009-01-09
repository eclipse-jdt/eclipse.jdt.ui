package p;

import java.util.List;

class A {
	void x() {
		List<? extends Number> nums = getNums();
		for (Number n : nums) {
			System.out.println(n);
		}
	}

	private List getNums() {
		return null;
	}
}
