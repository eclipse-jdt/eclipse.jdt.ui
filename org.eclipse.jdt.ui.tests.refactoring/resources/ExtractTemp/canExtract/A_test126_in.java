package p; //6, 32, 6, 58

class A {
	void foo(Object obj) {
		if (obj instanceof Integer && ((Integer) obj).intValue() > 0) {
			System.out.println(((Integer) obj).intValue());
		} else if (obj instanceof Float && ((Float) obj).floatValue() > 0.0) {
			System.out.println(((Float) obj).floatValue());
		}

	}
}
